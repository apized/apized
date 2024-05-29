package org.apized.micronaut.messaging.rabbitmq;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.micronaut.tracing.TraceUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
@Requires(bean = Tracer.class)
@Replaces(RabbitMQESBAdapter.class)
public class TracedRabbitMQESBAdapter extends RabbitMQESBAdapter {

  @Inject
  Tracer tracer;

  @Override
  public void send(UUID messageId, Date timestamp, String topic, Map<String, Object> headers, Object payload) {
    TraceUtils.wrap(
      tracer,
      "TracedRabbitMQESBAdapter::send",
      SpanKind.PRODUCER,
      (builder) -> {
        builder.setAttribute("messaging.system", "rabbitmq");
        builder.setAttribute("messaging.operation.type", "publish");
        builder.setAttribute("messaging.destination.name", topic);
      },
      () -> {
        Span span = Span.current();
        SpanContext spanContext = span.getSpanContext();
        Map<String, Object> enrichedHeaders = new HashMap<>(headers);
        enrichedHeaders.put("traceId", spanContext.getTraceId());
        enrichedHeaders.put("spanId", spanContext.getSpanId());

        try {
          span.setAttribute("messaging.message", mapper.writeValueAsString(Map.of("header", enrichedHeaders, "payload", payload)));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        super.send(messageId, timestamp, topic, enrichedHeaders, payload);
        return null;
      }
    );
  }
}
