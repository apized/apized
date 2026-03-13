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

package org.apized.spring.server.serde;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.spring.core.ApizedServerFilter;
import org.springframework.data.util.StreamUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SerdeFilter extends ApizedServerFilter {
  private final Pattern urlPattern = Pattern.compile("/(\\w+)(/(\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}))?");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (shouldExclude(request.getServletPath())) return;

    ApizedContext.getRequest().setPathVariables(getPathVariables(request));
    ApizedContext.getRequest().setFields(getParameters(request, "fields"));
    ApizedContext.getRequest().setSearch(getParameters(request, "search"));
    ApizedContext.getRequest().setSort(getParameters(request, "sort"));
    ApizedContext.getRequest().setQueryParams(request.getParameterMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (e) -> List.of(e.getValue()))));
    ApizedContext.getRequest().setHeaders(StreamUtils.createStreamFromIterator(request.getHeaderNames().asIterator()).collect(Collectors.toMap((e) -> e, (e) -> StreamUtils.createStreamFromIterator(request.getHeaders(e).asIterator()).toList())));

    filterChain.doFilter(request, response);

//    if (request.getMethod().equals(HttpMethod.GET.toString()) && request.getServletPath().equals("/") && !ApizedContext.getRequest().getFields().containsKey("content")) {
//      ApizedContext.getRequest().setFields(Map.of("*", Map.of(), "content", ApizedContext.getRequest().getFields().isEmpty() ? Map.of("*", Map.of()) : ApizedContext.getRequest().getFields()));
//      ApizedContext.getRequest().setSearch(Map.of("content", ApizedContext.getRequest().getSearch()));
//      ApizedContext.getRequest().setSort(Map.of("content", ApizedContext.getRequest().getSort()));
//    }
  }

  private Map<String, UUID> getPathVariables(HttpServletRequest request) {
    Map<String, UUID> variables = new HashMap<>();

    Matcher matcher = urlPattern.matcher(request.getServletPath());
    while (matcher.find()) {
      variables.put(
        StringHelper.singularize(matcher.group(1)),
        StringHelper.convertStringToUUID(matcher.group(3))
      );
    }

    return variables;
  }

  Map<String, Object> getParameters(HttpServletRequest request, String param) {
    Map<String, Object> params = new HashMap<>();
    for (String parameter : Optional.ofNullable(request.getParameterMap().get(param)).map(p -> Arrays.stream(p).toList()).orElse(List.of())) {
      for (String path : parameter.split(",")) {
        Map<String, Object> loc = params;
        for (String it : path.trim().split("\\.")) {
          if (!loc.containsKey(it)) {
            loc.put(it, new HashMap<>());
          }
          loc = (Map<String, Object>) loc.get(it);
        }
      }
    }
    return params;
  }

  @Override
  public int getOrder() {
    return 3;
  }
}
