package org.apized.micronaut.core;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.model.Model;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Slf4j
@ServerFilter(Filter.MATCH_ALL_PATTERN)
class ETagFilter extends ApizedServerFilter {
  @Inject
  ObjectMapper mapper;

  MessageDigest md;

  @SneakyThrows
  public ETagFilter() {
    md = MessageDigest.getInstance("MD5");
  }

  @ResponseFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterResponse(HttpRequest<?> request, MutableHttpResponse<?> response) {
    if (!shouldExclude(request.getPath()) && HttpMethod.GET.equals(request.getMethod())) {
      String previousToken = request.getHeaders().get("If-None-Match");
      response.getBody().ifPresent(obj -> {
        String payload = obj.toString();
        if (obj instanceof Model) {
          try {
            payload = mapper.writeValueAsString(obj);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        String tokenHeader = String.format(
          "W/\"%s\"",
          new BigInteger(1, md.digest((payload).getBytes(StandardCharsets.UTF_8))).toString(16)
        );
        if (previousToken != null && previousToken.equals(tokenHeader)) {
          response.status(HttpStatus.NOT_MODIFIED);
          response.getHeaders().set("Last-Modified", request.getHeaders().get("If-Modified-Since"));
        } else {
          response.getHeaders().set("ETag", tokenHeader);
          response.getHeaders().set("Last-Modified", LocalDateTime.now().toString());
        }
      });
    }
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.TRACING.order();
  }
}
