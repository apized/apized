package org.apized.micronaut.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TraceUtils {

  public static <T> T wrap(Tracer tracer, String name, SpanKind kind, Consumer<SpanBuilder> spanCustomizer, Supplier<T> execution) {
    SpanBuilder spanBuilder = tracer
      .spanBuilder(name)
      .setSpanKind(kind);

    spanCustomizer.accept(spanBuilder);

    Span span = spanBuilder.startSpan();
    try (Scope ignore = span.makeCurrent()) {
      return execution.get();
    } catch (Throwable t) {
      span.setStatus(StatusCode.ERROR);
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      span.recordException(
        t,
        Attributes.of(
          AttributeKey.booleanKey("exception.escaped"), true,
          AttributeKey.stringKey("exception.message"), t.getMessage(),
          AttributeKey.stringKey("exception.stacktrace"), sw.toString(),
          AttributeKey.stringKey("exception.type"), t.getClass().getName()
        )
      );
      throw t;
    } finally {
      span.end();
    }
  }
}
