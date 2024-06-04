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

package org.apized.micronaut.server.serde;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.micronaut.core.ApizedServerFilter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ServerFilter(Filter.MATCH_ALL_PATTERN)
public class SerdeFilter extends ApizedServerFilter {
  private final Pattern urlPattern = Pattern.compile("/(\\w+)(/(\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}))?");

  @RequestFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterRequest(HttpRequest<?> request) {
    if (shouldExclude(request.getPath())) return;

    ApizedContext.getRequest().setPathVariables(getPathVariables(request));
    ApizedContext.getRequest().setFields(getParameters(request, "fields"));
    ApizedContext.getRequest().setSearch(getParameters(request, "search"));
    ApizedContext.getRequest().setSort(getParameters(request, "sort"));
    ApizedContext.getRequest().setReason(request.getHeaders().get("X-Reason"));
    ApizedContext.getRequest().setQueryParams(request.getParameters().asMap());
  }

  private Map<String, UUID> getPathVariables(HttpRequest<?> request) {
    Map<String, UUID> variables = new HashMap<>();

    Matcher matcher = urlPattern.matcher(request.getPath());
    while (matcher.find()) {
      variables.put(
        StringHelper.singularize(matcher.group(1)),
        StringHelper.convertStringToUUID(matcher.group(3))
      );
    }

    return variables;
  }

  Map<String, Object> getParameters(HttpRequest<?> request, String param) {
    Map<String, Object> params = new HashMap<>();
    for (String parameter : Optional.ofNullable(request.getParameters().getAll(param)).orElse(List.of())) {
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
    return ServerFilterPhase.FIRST.after() + 1;
  }
}
