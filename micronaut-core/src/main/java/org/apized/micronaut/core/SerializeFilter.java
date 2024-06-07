package org.apized.micronaut.core;

import io.micronaut.http.HttpRequest;
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
import java.security.MessageDigest;

@Slf4j
@ServerFilter(Filter.MATCH_ALL_PATTERN)
class SerializeFilter extends ApizedServerFilter {
  @Inject
  ObjectMapper mapper;

  MessageDigest md;

  @SneakyThrows
  public SerializeFilter() {
    md = MessageDigest.getInstance("MD5");
  }

  @ResponseFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterResponse(HttpRequest<?> request, MutableHttpResponse<?> response) {
    if (!shouldExclude(request.getPath())) {
      response.getBody().ifPresent(obj -> {
        try {
          response.body(mapper.writeValueAsBytes(obj));
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
          log.info("{} on {} - {}", e.getMessage(), request.getPath(), obj);
        }
      });
    }
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.TRACING.after();
  }
}
