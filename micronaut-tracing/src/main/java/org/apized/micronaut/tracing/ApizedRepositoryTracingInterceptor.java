package org.apized.micronaut.tracing;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.ApizedConfig;
import org.apized.micronaut.server.mvc.ApizedRepository;

import java.io.PrintWriter;
import java.io.StringWriter;

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
    Span span = tracer
      .spanBuilder(operation)
      .setSpanKind(SpanKind.CLIENT)
      .setAttribute("db.system", apizedConfig.getDialect().toString().toLowerCase())
      .setAttribute("db.operation", operation)
      .startSpan();

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
