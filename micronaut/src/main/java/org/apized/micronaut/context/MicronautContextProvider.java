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

package org.apized.micronaut.context;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.apized.core.context.*;

import java.util.Optional;

public class MicronautContextProvider implements ContextProvider {
  ThreadLocal<ApizedContext> threadLocal = ThreadLocal.withInitial(this::getNew);

  @Override
  public ApizedContext get() {
    ApizedContext context;
    Optional<HttpRequest<Object>> request = ServerRequestContext.currentRequest();
    if (request.isPresent()) {
      Optional<ApizedContext> apizedContext = request.get().getAttribute("apizedContext", ApizedContext.class);
      if (apizedContext.isEmpty()) {
        context = getNew();
        request.get().setAttribute("apizedContext", context);
      } else {
        context = apizedContext.get();
      }
    } else {
      context = threadLocal.get();
    }
    return context;
  }

  @Override
  public void destroy() {
    threadLocal.remove();
  }

  private ApizedContext getNew() {
    return new ApizedContext(
      new RequestContext(),
      new SecurityContext(),
      new SerdeContext(),
      new FederationContext(),
      new AuditContext(),
      new EventContext()
    );
  }
}
