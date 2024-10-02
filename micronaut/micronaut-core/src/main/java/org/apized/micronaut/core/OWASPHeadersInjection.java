package org.apized.micronaut.core;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import org.reactivestreams.Publisher;

@Filter("/**")
class OWASPHeadersInjection implements HttpServerFilter {

  @Override
  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    return Publishers.then(chain.proceed(request), response -> {
      response.getHeaders().set("X-Content-Type-Options", "nosniff");
      response.getHeaders().set("Strict-Transport-Security", "max-age=31536000");
      response.getHeaders().set("Content-Security-Policy", "script-src 'self' 'unsafe-inline'");
    });
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.RENDERING.after();
  }
}
