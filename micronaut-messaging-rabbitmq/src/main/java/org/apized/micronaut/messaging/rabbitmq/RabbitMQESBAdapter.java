package org.apized.micronaut.messaging.rabbitmq;

import com.rabbitmq.client.AMQP;
import io.micronaut.context.annotation.Value;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.event.ESBAdapter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
  public void send(UUID messageId, Date timestamp, String topic, Map<String, Object> headers, Object payload) {
    enrichers.forEach(it -> it.process(topic, headers, payload));
    try {
      pool.getChannel().basicPublish(
        exchange,
        topic,
        new AMQP.BasicProperties.Builder()
          .messageId(messageId.toString())
          .contentType("application/json")
          .timestamp(timestamp)
          .headers(headers)
          .build(),
        mapper.writeValueAsBytes(Map.of("header", headers, "payload", payload))
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
