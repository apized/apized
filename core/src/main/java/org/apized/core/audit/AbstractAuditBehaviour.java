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

package org.apized.core.audit;

import org.apized.core.ModelMapper;
import org.apized.core.ScopeHelper;
import org.apized.core.audit.annotation.AuditField;
import org.apized.core.audit.annotation.AuditIgnore;
import org.apized.core.audit.model.AuditEntry;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.context.ApizedContext;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Model;
import org.apized.core.model.When;

import java.util.ArrayList;
import java.util.Map;

public abstract class AbstractAuditBehaviour implements BehaviourHandler<Model> {

  private final ModelMapper modelMapper = new ModelMapper(AuditField.class, AuditIgnore.class);

  @Override
  public void process(Class<Model> type, When when, Action action, Execution<Model> execution) {
    Model model = execution.getOutput();
    ScopeHelper.scopeUpUntil(
      model,
      a -> a.booleanValue("audit").orElse(true),
      i -> ApizedContext.getAudit().add(AuditEntry.builder()
        .transactionId(ApizedContext.getRequest().getId())
        .action(type.isAssignableFrom(i.getClass()) ? action : Action.UPDATE)
        .type(i.getClass().getSimpleName().replaceAll("\\$Proxy$", ""))
        .author(ApizedContext.getSecurity().getUser().getId())
        .reason(ApizedContext.getRequest().getHeaders().getOrDefault("X-Reason", new ArrayList<>()).isEmpty() ? null : ApizedContext.getRequest().getHeaders().get("X-Reason").getFirst())
        .target(i.getId())
        .payload(action != Action.DELETE ? modelMapper.createMapOf(i) : Map.of())
        .timestamp(ApizedContext.getRequest().getTimestamp())
        .epoch(System.nanoTime())
        .build()
      )
    );
  }
}
