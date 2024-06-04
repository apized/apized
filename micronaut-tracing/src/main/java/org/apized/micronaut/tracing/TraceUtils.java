package org.apized.micronaut.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.function.Function;

public class TraceUtils {

  public static <T> T wrap(Tracer tracer, String name, SpanKind kind, Function<Span,T> execution) {
    return TraceUtils.wrap(tracer, name, kind,(a)->{}, execution, (b)->{});
  }

  public static <T> T wrap(Tracer tracer, String name, SpanKind kind, Consumer<SpanBuilder> spanBuilder, Function<Span,T> execution) {
    return TraceUtils.wrap(tracer, name, kind, spanBuilder, execution, (b)->{});
  }

  public static <T> T wrap(Tracer tracer, String name, SpanKind kind, Consumer<SpanBuilder> spanBuilder, Function<Span,T> execution, Consumer<Span> spanFinalizer) {
    SpanBuilder builder = tracer
      .spanBuilder(name)
      .setSpanKind(kind);

    spanBuilder.accept(builder);

    Span span = builder.startSpan();
    try (Scope ignore = span.makeCurrent()) {
      T t = execution.apply(span);
      span.setStatus(StatusCode.OK);
      return t;
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
      spanFinalizer.accept(span);
      span.end();
    }
  }
}
