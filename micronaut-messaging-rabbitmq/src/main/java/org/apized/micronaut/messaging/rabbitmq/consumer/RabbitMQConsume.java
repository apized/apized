package org.apized.micronaut.messaging.rabbitmq.consumer;

public @interface RabbitMQConsume {
  String exchange();

  String queue();

  String[] bindings();
}
