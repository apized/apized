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

package org.apized.micronaut.audit;

import io.micronaut.context.annotation.Context;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.audit.AbstractAuditBehaviour;
import org.apized.core.audit.model.AuditEntry;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.context.ApizedContext;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;

import java.io.IOException;
import java.util.List;

@Slf4j
@Context
public class AuditBehaviour extends AbstractAuditBehaviour {
  @Inject
  ObjectMapper mapper;

  @Inject
  AuditEntryRepository repository;

  public AuditBehaviour(BehaviourManager manager) {
    manager.registerBehaviour(Model.class, Layer.SERVICE, List.of(When.AFTER), List.of(Action.CREATE, Action.UPDATE, Action.DELETE), 1000, this);
  }

  @Override
  protected void save(AuditEntry entry) {
    repository.save(entry);
    if (log.isDebugEnabled()) {
      ApizedContext.getAudit().getAuditEntries().values().forEach((it) ->
        {
          try {
            log.debug("{}[{}]: {}", it.getTarget(), it.getId(), mapper.writeValueAsString(it.getPayload()));
          } catch (IOException e) {
            //Do nothing
          }
        }
      );
    }
  }
}
