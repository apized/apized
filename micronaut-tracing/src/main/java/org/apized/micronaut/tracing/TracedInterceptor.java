package org.apized.micronaut.tracing;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.tracing.annotation.SpanTag;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.tracing.TraceKind;
import org.apized.core.tracing.Traced;

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

    SpanKind kind = SpanKind.valueOf(traced.enumValue("kind", TraceKind.class).orElse(TraceKind.INTERNAL).name());

    return TraceUtils.wrap(
      tracer,
      name,
      kind,
      (spanBuilder) -> {
        if (kind.equals(SpanKind.CONSUMER)) {
          spanBuilder.setAttribute("messaging.consumer_id", context.getDeclaringType().getSimpleName());
        }

        context.getParameters().forEach((key, value) ->
          Optional.ofNullable(value.getAnnotation(SpanTag.class)).ifPresent(annotation -> {
              String attrName = annotation.stringValue("value").orElse("");
              if (attrName.isBlank()) {
                attrName = key;
              }
              spanBuilder.setAttribute(attrName, value.toString());
            }
          ));

        traced.getAnnotations("attributes", Traced.Attribute.class).forEach(attr -> {
          String value = attr.stringValue("value").orElse("");
          String arg = attr.stringValue("arg").orElse("");
          spanBuilder.setAttribute(
            attr.stringValue("key").orElse(""),
            !arg.isBlank() && context.getParameters().containsKey(arg) ? context.getParameters().get(arg).getValue().toString() : value
          );
        });
      },
      context::proceed
    );
  }
}
