package org.apized.micronaut.messaging.rabbitmq;

import java.util.Map;

public interface EventContextEnricher {
  void process(String topic, Map<String, Object> headers, Object payload);
}
