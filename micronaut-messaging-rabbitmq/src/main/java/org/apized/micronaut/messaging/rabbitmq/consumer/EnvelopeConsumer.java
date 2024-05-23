package org.apized.micronaut.messaging.rabbitmq.consumer;

import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.type.Argument;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.serde.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.ApizedConfig;
import org.apized.core.context.ApizedContext;
import org.apized.core.error.ExceptionNotifier;
import org.apized.core.security.UserResolver;

import java.io.IOException;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@Slf4j
@Introspected
public abstract class EnvelopeConsumer<T> {
  protected Map<String, Integer> registry = new HashMap<>();

  protected final QueueConfig queue;

  protected ChannelPool pool;

  protected ObjectMapper mapper;

  protected UserResolver resolver;

  protected ApizedConfig config;

  protected ApplicationEventPublisher<String> eventPublisher;

  protected List<ExceptionNotifier> notifiers;

  public EnvelopeConsumer(QueueConfig queue, ChannelPool pool, ObjectMapper mapper, UserResolver resolver, ApizedConfig config, ApplicationEventPublisher<String> eventPublisher, List<ExceptionNotifier> notifiers) {
    this.queue = queue;
    this.pool = pool;
    this.mapper = mapper;
    this.resolver = resolver;
    this.config = config;
    this.eventPublisher = eventPublisher;
    this.notifiers = notifiers;

    try {
      Channel channel = pool.getChannel();
      this.setup(channel);
      pool.returnChannel(channel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setup(Channel channel) throws IOException {
    String deadLetterExchange = String.format("%s.dead-letter", queue.getExchange());

    channel.exchangeDeclare(queue.getExchange(), BuiltinExchangeType.TOPIC, true);
    channel.exchangeDeclare(deadLetterExchange, BuiltinExchangeType.TOPIC, true);

    channel.queueDeclare(queue.getName() + "-error", true, false, false, Map.of());
    channel.queueBind(queue.getName() + "-error", deadLetterExchange, queue.getName());

    channel.queueDeclare(queue.getName(), true, false, false, Map.of(
      "x-dead-letter-exchange", deadLetterExchange,
      "x-dead-letter-routing-key", queue.getName()
    ));

    for (String routingKey : queue.getBindings()) {
      channel.queueBind(queue.getName(), queue.getExchange(), routingKey);
    }
  }

  @SuppressWarnings("unchecked")
  protected void consume(byte[] data, Envelope envelope, BasicProperties properties, RabbitAcknowledgement acknowledgement) {
    String messageId = Optional.ofNullable(properties.getMessageId()).orElseGet(() -> checksum(data));
    log.info("Processing incoming message {} on `{}` with: {}", messageId, envelope.getRoutingKey(), new String(data));
    long start = System.currentTimeMillis();

    try {
      Map<String, Object> message = Optional.ofNullable(mapper.readValue(data, Argument.of(Map.class, String.class, Object.class))).orElseGet(HashMap::new);
      Map<String, Object> header = (Map<String, Object>) message.getOrDefault("header", new HashMap<>());
      T payload = (T) message.get("payload");
      Date timestamp = Optional.ofNullable(properties.getTimestamp())
        .orElseGet(() ->
          header.containsKey("timestamp")
            ? new Date(((Number) header.get("timestamp")).longValue())
            : new Date()
        );

      ApizedContext.destroy();

      if (header.containsKey("token")) {
        ApizedContext.getSecurity().setUser(resolver.getUser((String) header.get("token")));
      } else {
        ApizedContext.getSecurity().setUser(resolver.getUser(config.getToken()));
      }

      getConsumer().process(
        envelope.getRoutingKey(),
        timestamp,
        header,
        Optional.ofNullable(payload).isPresent()
          ? (T) mapper.readValue(mapper.writeValueAsString(payload), getType())
          : (T) mapper.readValue(data, getType())
      );

      eventPublisher.publishEvent(this.getClass().getSuperclass().getSimpleName() + "::process");
      acknowledgement.ack(false);
      registry.remove(messageId);
      log.info("Message {} processed in {}ms", messageId, System.currentTimeMillis() - start);
    } catch (Throwable t) {
      log.error(t.getMessage(), t);
      notifiers.forEach(n -> n.report(t));
      log.info("Message {} failed: {}", messageId, t.getMessage());
      int attempt = registry.getOrDefault(messageId, 0) + 1;
      registry.put(messageId, attempt);
      try {
        Thread.sleep(1000L * attempt);
        acknowledgement.nack(false, attempt < 3);
      } catch (InterruptedException e) {
        notifiers.forEach(n -> n.report(e));
        throw new RuntimeException(e);
      }
      throw new RuntimeException(t);
    }
  }

  protected abstract RabbitMQConsumer<T> getConsumer();

  protected abstract Argument<?> getType();

  private String checksum(byte[] data) {
    Checksum crc32 = new CRC32();
    crc32.update(data, 0, data.length);
    return Long.toString(crc32.getValue());
  }
}
