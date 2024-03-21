package org.apized.micronaut.tracing;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.tracing.annotation.ContinueSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.apized.core.context.ApizedContext;
import org.apized.micronaut.core.ApizedHttpServerFilter;
import org.reactivestreams.Publisher;

@Requires(bean = Tracer.class)
@Filter("/**")
public class TracingUserEnricher extends ApizedHttpServerFilter {
  @Override
  @ContinueSpan
  public Publisher<MutableHttpResponse<?>> filter(HttpRequest<?> request, ServerFilterChain chain) {
    Span.current().setAttribute("user", ApizedContext.getSecurity().getUser().getId().toString());
    return chain.proceed(request);
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.SECURITY.after();
  }
}
