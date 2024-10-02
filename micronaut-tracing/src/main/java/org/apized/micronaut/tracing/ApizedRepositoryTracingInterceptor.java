package org.apized.micronaut.tracing;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.ApizedConfig;
import org.apized.core.mvc.ApizedRepository;

@Singleton
@Requires(bean = Tracer.class)
@InterceptorBean(ApizedRepository.class)
public class ApizedRepositoryTracingInterceptor implements MethodInterceptor<Object, Object> {

  @Inject
  ApizedConfig apizedConfig;

  @Inject
  Tracer tracer;

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    String operation = String.format("%s::%s", context.getTarget().getClass().getSimpleName(), context.getMethodName());

    return TraceUtils.wrap(
      tracer,
      operation,
      SpanKind.INTERNAL,
      (spanBuilder) ->
        spanBuilder.setAttribute("db.system", apizedConfig.getDialect().toString().toLowerCase())
          .setAttribute("db.operation", operation),
      (span) -> context.proceed()
    );
  }
}
