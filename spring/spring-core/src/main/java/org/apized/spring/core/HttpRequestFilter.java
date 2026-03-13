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

package org.apized.spring.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.context.ApizedContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Slf4j
@Component
public class HttpRequestFilter extends ApizedServerFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (shouldExclude(request.getServletPath())) {
      filterChain.doFilter(request, response);
      return;
    }

    ApizedContext.destroy();
    log.info("Request {} started: {} {}", ApizedContext.getRequest().getId(), request.getMethod(), request.getServletPath());

    filterChain.doFilter(request, response);

    long start = ApizedContext.getRequest().getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli();
    long end = LocalDateTime.now(ZoneId.of("UTC")).toInstant(ZoneOffset.UTC).toEpochMilli();
    log.info("Request {} took {}ms", ApizedContext.getRequest().getId(), end - start);
  }

  @Override
  public int getOrder() {
    return 1;
  }
}
