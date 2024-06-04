/*
 * Copyright 2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apized.micronaut.core;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.context.ApizedContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@ServerFilter(Filter.MATCH_ALL_PATTERN)
public class HttpRequestFilter extends ApizedServerFilter {

  @RequestFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterRequest(HttpRequest<?> request) {
    if (shouldExclude(request.getPath())) return;

    ApizedContext.destroy();
    log.info("Request {} started: {} {}", ApizedContext.getRequest().getId(), request.getMethod(), request.getPath());
  }

  @ResponseFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterResponse(HttpRequest<?> request) {
    if (shouldExclude(request.getPath())) return;

    long start = ApizedContext.getRequest().getTimestamp().toInstant().toEpochMilli();
    long end = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))).toInstant().toEpochMilli();
    log.info("Request {} took {}ms", ApizedContext.getRequest().getId(), end - start);
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.FIRST.after();
  }
}
