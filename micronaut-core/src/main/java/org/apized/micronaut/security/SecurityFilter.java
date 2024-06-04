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

package org.apized.micronaut.security;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.http.simple.cookies.SimpleCookie;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.ApizedConfig;
import org.apized.core.context.ApizedContext;
import org.apized.core.security.UserResolver;
import org.apized.micronaut.core.ApizedServerFilter;

@Slf4j
@ServerFilter(Filter.MATCH_ALL_PATTERN)
@Requires(bean = UserResolver.class)
public class SecurityFilter extends ApizedServerFilter {
  @Inject
  protected ApizedConfig config;

  @Inject
  UserResolver resolver;

  @RequestFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterRequest(HttpRequest<?> request) {
    if (shouldExclude(request.getPath())) return;

    try {
      String authorization = request.getHeaders().get("Authorization");
      String token;

      if (authorization != null) {
        token = authorization.replaceAll("Bearer (.*)", "$1");
      } else {
        token = request.getCookies().findCookie(config.getCookie()).orElse(new SimpleCookie(config.getCookie(), null)).getValue();
      }
      ApizedContext.getSecurity().setToken(token);
      long start = System.currentTimeMillis();
      ApizedContext.getSecurity().setUser(resolver.getUser(token));
      log.info("User {} resolved in {} ms for {}", ApizedContext.getSecurity().getUser() != null ? ApizedContext.getSecurity().getUser().getUsername() : null, System.currentTimeMillis() - start, request.getPath());
    } catch (Exception ignored) {
      log.error(ignored.getMessage(), ignored);
    }
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.SECURITY.order();
  }
}
