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

package org.apized.spring.security;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.ApizedConfig;
import org.apized.core.context.ApizedContext;
import org.apized.core.security.UserResolver;
import org.apized.spring.core.ApizedServerFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@ConditionalOnBean(UserResolver.class)
@Component
public class SecurityFilter extends ApizedServerFilter {
  @Inject
  protected ApizedConfig config;

  @Inject
  UserResolver resolver;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (shouldExclude(request.getServletPath())) return;

    try {
      String authorization = request.getHeader("Authorization");
      String token;

      if (authorization != null) {
        token = authorization.replaceAll("Bearer (.*)", "$1");
      } else {
        token = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[]{}))
          .filter(c -> c.getName().equals(config.getCookie())).findFirst()
          .map(Cookie::getValue)
          .orElse(null);
      }
      ApizedContext.getSecurity().setToken(token);
      long start = System.currentTimeMillis();
      ApizedContext.getSecurity().setUser(resolver.getUser(token));
      log.info("User {} resolved in {} ms for {}", ApizedContext.getSecurity().getUser() != null ? ApizedContext.getSecurity().getUser().getUsername() : null, System.currentTimeMillis() - start, request.getServletPath());
    } catch (Exception ignored) {
      log.error(ignored.getMessage(), ignored);
    }

    filterChain.doFilter(request, response);
  }

  @Override
  public int getOrder() {
    return 2;
  }
}
