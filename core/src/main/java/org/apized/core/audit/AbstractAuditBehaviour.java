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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import org.apized.core.ModelMapper;
import org.apized.core.ScopeHelper;
import org.apized.core.audit.annotation.*;
import org.apized.core.audit.annotation.AuditField;
import org.apized.core.audit.annotation.AuditIgnore;
import org.apized.core.audit.model.AuditEntry;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.execution.Execution;
import org.apized.core.model.*;
import org.apized.core.security.SecurityContext;
import org.apized.core.serde.RequestContext;

import java.util.*;

public abstract class AbstractAuditBehaviour implements BehaviourHandler<Model> {

  private final ModelMapper modelMapper = new ModelMapper(AuditField.class, AuditIgnore.class);

  @Override
  public void process(Class<Model> type, When when, Action action, Execution<Model> execution) {
    Model model = execution.getInput();
    ScopeHelper.scopeUpUntil(
      model,
      a -> a.booleanValue("audit").orElse(true),
      i -> AuditContext.getInstance().add(AuditEntry.builder()
        .transactionId(RequestContext.getInstance().getId())
        .action(type.equals(i.getClass()) ? action : Action.UPDATE)
        .type(type.getSimpleName())
        .by(SecurityContext.getInstance().getUser().getId())
        .reason(RequestContext.getInstance().getReason())
        .target(model.getId())
        .payload((type.equals(i.getClass()) ? action : Action.UPDATE) != Action.DELETE ? modelMapper.createMapOf(model) : Map.of())
        .timestamp(RequestContext.getInstance().getTimestamp())
        .epoch(System.nanoTime())
        .build()
      )
    );
  }
}
