package org.apized.micronaut.core;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import java.time.LocalDateTime;

@Filter("/**")
class ETagFilter implements HttpServerFilter {

  @Override
  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    return Publishers.then(chain.proceed(request), response -> {
      if (HttpMethod.GET.equals(request.getMethod())) {
        String previousToken = request.getHeaders().get("If-None-Match");
        response.getBody(String.class).ifPresent(obj -> {
          StringBuilder queryString = new StringBuilder();
          request.getParameters().asMap().forEach((k, v) -> queryString.append(String.format("%s=%s", k, String.join(",", v))));

          String token = Integer.toHexString(obj.hashCode()).toUpperCase();
          String tokenHeader = String.format("W/\"%s:%s\"", queryString, token);

          if (previousToken != null && previousToken.equals(tokenHeader)) {
            response.status(HttpStatus.NOT_MODIFIED);
            response.getHeaders().set("Last-Modified", request.getHeaders().get("If-Modified-Since"));
          } else {
            response.getHeaders().set("ETag", tokenHeader);
            response.getHeaders().set("Last-Modified", LocalDateTime.now().toString());
          }
        });
      }
    });
  }
}
