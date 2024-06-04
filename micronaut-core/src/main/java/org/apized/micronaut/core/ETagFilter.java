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

import java.io.IOException;
import java.math.BigInteger;
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
    if (shouldExclude(request.getPath())) return;

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
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
          log.info("{} on {} - {}", e.getMessage(), request.getPath(), obj);
        }
      });
    }
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.RENDERING.after();
  }
}
