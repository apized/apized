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

package org.apized.spring.context;

import org.apized.core.context.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

public class SpringContextProvider implements ContextProvider {
  ThreadLocal<ApizedContext> threadLocal = ThreadLocal.withInitial(this::getNew);

  @Override
  public ApizedContext get() {
    ApizedContext context;

    try {
      var requestAttributes = RequestContextHolder.currentRequestAttributes();
      Optional<ApizedContext> apizedContext = Optional.ofNullable((ApizedContext) requestAttributes.getAttribute("apizedContext", RequestAttributes.SCOPE_REQUEST));
      if (apizedContext.isEmpty()) {
        context = threadLocal.get();
        requestAttributes.setAttribute("apizedContext", context, RequestAttributes.SCOPE_REQUEST);
      } else {
        context = apizedContext.get();
      }
    } catch (IllegalStateException e) {
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
