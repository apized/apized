package org.apized.micronaut.messaging.rabbitmq.consumer;

public @interface RabbitMQConsume {
  String exchange();

  String queue();

  String[] bindings();

  /**
   * Indicated if this consumer should be traced. Requires apized.tracing to be added to the project
   */
  boolean traced() default true;

  /**
   * Indicated if this consumer should have metrics. Requires micronaut.micrometer to be added to the project.
   * Add the metrics annotation processor: `annotationProcessor("io.micronaut.micrometer:micronaut-micrometer-annotation")`
   */
  boolean metrics() default true;
}
