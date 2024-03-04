package org.apized.micronaut.core;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.regex.Pattern;

public abstract class ApizedHttpServerFilter implements HttpServerFilter {

  @Inject
  protected ApizedConfig config;

  @Override
  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    if (shouldExclude(request.getPath())) {
      return chain.proceed(request);
    } else {
      return filter(request, chain);
    }
  }

  abstract public Publisher<MutableHttpResponse<?>> filter(HttpRequest<?> request, ServerFilterChain chain);

  public @Nullable boolean shouldExclude(String uri) {
    if (CollectionUtils.isEmpty(config.getExclusions())) {
      return false;
    } else {
      List<Pattern> patterns = config.getExclusions().stream().map(Pattern::compile).toList();
      return patterns.stream().anyMatch((pattern) -> pattern.matcher(uri).matches());
    }
  }
}
