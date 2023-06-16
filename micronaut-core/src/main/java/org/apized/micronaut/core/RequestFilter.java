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

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.context.ApizedContext;
import org.reactivestreams.Publisher;

@Slf4j
@Filter("/**")
public class RequestFilter implements HttpServerFilter {
  @Override
  public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
    if (!request.getPath().equals("/") && !request.getPath().startsWith("/health")) {
      long start = System.currentTimeMillis();
      ApizedContext.destroy();
      log.info("Request {} started: {}", ApizedContext.getRequest().getId(), request.getPath());
      return Publishers.then(
        chain.proceed(request),
        response -> log.info("Request {} took {}ms", ApizedContext.getRequest().getId(), System.currentTimeMillis() - start)
      );
    } else {
      return chain.proceed(request);
    }
  }

  @Override
  public int getOrder() {
    return ServerFilterPhase.FIRST.after();
  }
}
