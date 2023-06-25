package org.apized.micronaut.messaging.rabbitmq.consumer;

import java.util.Date;
import java.util.Map;

public interface RabbitMQConsumer {
  void process(String topic, Date timestamp, Map<String, Object> header, Map<String, Object> payload);
}
