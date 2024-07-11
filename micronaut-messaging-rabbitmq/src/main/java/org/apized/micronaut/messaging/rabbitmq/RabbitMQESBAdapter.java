package org.apized.micronaut.messaging.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.micronaut.context.annotation.Value;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.event.ESBAdapter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Singleton
public class RabbitMQESBAdapter implements ESBAdapter {

  @Value("${rabbitmq.exchange:apized}")
  private String exchange;

  @Inject
  ChannelPool pool;

  @Inject
  ObjectMapper mapper;

  @Inject
  List<EventContextEnricher> enrichers;

  @Override
  public void send(UUID messageId, LocalDateTime timestamp, String topic, Map<String, Object> headers, Object payload) {
    enrichers.forEach(it -> it.process(topic, headers, payload));
    try {
      Channel channel = pool.getChannel();
      channel.basicPublish(
        exchange,
        topic,
        new AMQP.BasicProperties.Builder()
          .messageId(messageId.toString())
          .contentType("application/json")
          .timestamp(Date.from(timestamp.toInstant(ZoneOffset.UTC)))
          .headers(headers)
          .build(),
        mapper.writeValueAsBytes(Map.of("header", headers, "payload", payload))
      );
      pool.returnChannel(channel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
