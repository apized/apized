package org.apized.spring.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apized.core.context.ApizedContext;
import org.apized.spring.core.ApizedServerFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(Tracer.class)
public class TracingUserEnricher extends ApizedServerFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
    if (shouldExclude(request.getServletPath())) return;

    if (ApizedContext.getSecurity().getUser() != null) {
      Span.current().setAttribute("user", ApizedContext.getSecurity().getUser().getId().toString());
    }
  }

  @Override
  public int getOrder() {
    return 3;
  }
}
