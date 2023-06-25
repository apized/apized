package org.apized.micronaut.messaging.rabbitmq.consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@AllArgsConstructor
public class QueueConfig {
  public QueueConfig(String exchange, String name, String... bindings) {
    this(exchange, name, List.of(bindings));
  }

  @NotNull
  private String exchange;

  @NotNull
  private String name;

  @NotNull
  private List<String> bindings;
}
