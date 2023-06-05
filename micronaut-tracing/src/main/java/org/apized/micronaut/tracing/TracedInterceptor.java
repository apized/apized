package org.apized.micronaut.tracing;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.tracing.annotation.SpanTag;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Singleton
@Requires(bean = Tracer.class)
@InterceptorBean(Traced.class)
public class TracedInterceptor implements MethodInterceptor<Object, Object> {
  @Inject
  Tracer tracer;

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    AnnotationValue<Traced> traced = context.getExecutableMethod().getAnnotation(Traced.class);
    assert traced != null;

    String name = traced.stringValue("value").orElse("");
    if (name.isBlank()) {
      name = String.format("%s::%s", context.getDeclaringType().getSimpleName(), context.getMethodName());
    }

    SpanKind kind = traced.enumValue("kind", SpanKind.class).orElse(SpanKind.SERVER);

    Span span = tracer
      .spanBuilder(name)
      .setSpanKind(kind)
      .startSpan();

    if (kind.equals(SpanKind.CONSUMER)) {
      span.setAttribute("messaging.consumer_id", context.getDeclaringType().getSimpleName());
    }

    context.getParameters().forEach((key, value) ->
      Optional.ofNullable(value.getAnnotation(SpanTag.class)).ifPresent(annotation -> {
          String attrName = annotation.stringValue("value").orElse("");
          if (attrName.isBlank()) {
            attrName = key;
          }
          span.setAttribute(attrName, value.toString());
        }
      ));

    traced.getAnnotations("attributes", Traced.Attribute.class).forEach(attr -> {
      span.setAttribute(
        attr.stringValue("key").orElse(""),
        attr.stringValue("value").orElse("")
      );
    });

    try (Scope ignore = span.makeCurrent()) {
      return context.proceed();
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
