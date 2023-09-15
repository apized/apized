package org.apized.micronaut.core;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Filter("/**")
class ETagFilter implements HttpServerFilter {
  @Inject
  ObjectMapper mapper;

  MessageDigest md;

  @SneakyThrows
  public ETagFilter() {
    md = MessageDigest.getInstance("MD5");
  }

  @Override
  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    return Publishers.then(chain.proceed(request), response -> {
      if (HttpMethod.GET.equals(request.getMethod())) {
        String previousToken = request.getHeaders().get("If-None-Match");
        response.getBody().ifPresent(obj -> {
          try {
            String tokenHeader = String.format(
              "W/\"%s\"",
              new BigInteger(1, md.digest(mapper.writeValueAsBytes(obj))).toString(16)
            );
            if (previousToken != null && previousToken.equals(tokenHeader)) {
              response.status(HttpStatus.NOT_MODIFIED);
              response.getHeaders().set("Last-Modified", request.getHeaders().get("If-Modified-Since"));
            } else {
              response.getHeaders().set("ETag", tokenHeader);
              response.getHeaders().set("Last-Modified", LocalDateTime.now().toString());
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }
    });
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.RENDERING.after();
  }
}
