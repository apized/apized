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

package org.apized.core.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@Getter
@AllArgsConstructor
public class ApizedContext {
  private final RequestContext requestContext;
  private final SecurityContext securityContext;
  private final SerdeContext serdeContext;
  private final FederationContext federationContext;
  private final AuditContext auditContext;
  private final EventContext eventContext;

  public static ContextProvider provider;

  public static RequestContext getRequest() {
    return provider.get().getRequestContext();
  }

  public static SecurityContext getSecurity() {
    return provider.get().getSecurityContext();
  }

  public static SerdeContext getSerde() {
    return provider.get().getSerdeContext();
  }

  public static FederationContext getFederation() {
    return provider.get().getFederationContext();
  }

  public static AuditContext getAudit() {
    return provider.get().getAuditContext();
  }

  public static EventContext getEvent() {
    return provider.get().getEventContext();
  }

  public static void destroy() {
    Optional.ofNullable(provider).ifPresent(ContextProvider::destroy);
  }
}
