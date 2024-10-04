package org.apized.spring.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.apized.core.tracing.Traced;
import org.apized.tracing.TraceUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@ConditionalOnBean(Tracer.class)
public class TracedInterceptor {
  final
  Tracer tracer;

  public TracedInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around("@annotation(org.apized.core.tracing.Traced)")
  public Object intercept(ProceedingJoinPoint call) throws Throwable {
    MethodSignature signature = (MethodSignature) call.getSignature();
    Method method = signature.getMethod();

    Traced traced = method.getAnnotation(Traced.class);
    assert traced != null;

    String name = traced.value();
    if (name.isBlank()) {
      name = String.format("%s::%s", method.getDeclaringClass().getSimpleName(), method.getName());
    }

    SpanKind kind = SpanKind.valueOf(traced.kind().name());

    Span parentSpan = Span.current();
    return TraceUtils.wrap(
      tracer,
      name,
      kind,
      (spanBuilder) -> {
        if (kind.equals(SpanKind.CONSUMER)) {
          spanBuilder.setAttribute("messaging.consumer_id", method.getDeclaringClass().getSimpleName());
        }

//        Arrays.stream(method.getParameters()).forEach((parameter) ->
//          Optional.ofNullable(parameter.getAnnotation(SpanTag.class)).ifPresent(annotation -> {
//              String attrName = annotation.value();
//              if (attrName.isBlank()) {
//                attrName = key;
//              }
//              spanBuilder.setAttribute(attrName, value.toString());
//            }
//          ));

        Arrays.stream(traced.attributes()).forEach(attr -> {
          String value = attr.value();
          String arg = attr.arg();
          spanBuilder.setAttribute(
            attr.key(),
            !arg.isBlank() && Arrays.stream(method.getParameters()).map(Parameter::getName).toList().contains(arg) ?
              List.of(call.getArgs()).get(Arrays.stream(method.getParameters()).map(Parameter::getName).toList().indexOf(arg)).toString()
              : value
          );
        });
      },
      (span) -> {
        //todo spring otel context propagation
//        PropagatedContext.Scope ignore = PropagatedContext.getOrEmpty()
//          .plus(new OpenTelemetryPropagationContext(Context.current().with(Span.current())))
//          .propagate();
        try {
          return call.proceed();
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      },
      (span) -> {
        //todo spring otel context propagation
//        PropagatedContext.Scope ignore = PropagatedContext.getOrEmpty()
//          .plus(new OpenTelemetryPropagationContext(Context.current().with(parentSpan)))
//          .propagate();
      }
    );
  }
}
