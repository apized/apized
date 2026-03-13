package org.apized.micronaut.tracing;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.tracing.annotation.ContinueSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.apized.core.context.ApizedContext;
import org.apized.micronaut.core.ApizedServerFilter;

@Requires(bean = Tracer.class)
@ServerFilter(Filter.MATCH_ALL_PATTERN)
public class TracingUserEnricher extends ApizedServerFilter {

  @ContinueSpan
  @RequestFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterRequest(HttpRequest<?> request) {
    if (shouldExclude(request.getPath())) return;

    if (ApizedContext.getSecurity().getUser() != null) {
      Span.current().setAttribute("user", ApizedContext.getSecurity().getUser().getId().toString());
    }
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.SECURITY.after();
  }
}
