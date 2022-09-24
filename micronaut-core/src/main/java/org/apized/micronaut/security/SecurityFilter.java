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

import org.apized.core.context.ApizedContext;
import org.apized.core.context.SecurityContext;
import org.apized.core.security.UserResolver;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.simple.cookies.SimpleCookie;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

@Slf4j
@Filter("/**")
@Requires(bean = UserResolver.class)
public class SecurityFilter implements HttpServerFilter {
  @Inject
  UserResolver resolver;

  @Override
  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    if (!request.getPath().startsWith("/health")) {
      try {
        String authorization = request.getHeaders().get("Authorization");
        String token;

        if (authorization != null) {
          token = authorization.replaceAll("Bearer (.*)", "$1");
        } else {
          token = request.getCookies().findCookie("token").orElse(new SimpleCookie("token", null)).getValue();
        }
        ApizedContext.getSecurity().setToken(token);
        long start = System.currentTimeMillis();
        ApizedContext.getSecurity().setUser(resolver.getUser(token));
        log.info("User {} resolved in {} ms for {}", ApizedContext.getSecurity().getUser() != null ? ApizedContext.getSecurity().getUser().getUsername() : null, System.currentTimeMillis() - start, request.getPath());
      } catch (Exception ignored) {
        log.error(ignored.getMessage(), ignored);
      }
    }

    return chain.proceed(request);
  }

  @Override
  public int getOrder() {
    return -100_000;
  }
}
