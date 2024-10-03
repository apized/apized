package org.apized.micronaut.tracing;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.apized.spring.tracing.TraceUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@ConditionalOnBean(Tracer.class)
public class ApizedRepositoryTracingInterceptor {

  @Inject
  ApizedConfig apizedConfig;

  @Inject
  Tracer tracer;

  @Around("@annotation(org.apized.core.mvc.ApizedRepository)")
  public Object intercept(ProceedingJoinPoint call) throws Throwable {
    MethodSignature signature = (MethodSignature) call.getSignature();
    Method method = signature.getMethod();

    String operation = String.format("%s::%s", method.getDeclaringClass().getSimpleName(), method.getName());

    return TraceUtils.wrap(
      tracer,
      operation,
      SpanKind.INTERNAL,
      (spanBuilder) ->
        spanBuilder.setAttribute("db.system", apizedConfig.getDialect().toString().toLowerCase())
          .setAttribute("db.operation", operation),
      (span) -> {
        try {
          return call.proceed();
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }
    );
  }
}
