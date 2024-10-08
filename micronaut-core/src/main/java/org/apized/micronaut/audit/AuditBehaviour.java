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
import org.apized.core.audit.AbstractAuditBehaviour;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;

import java.util.List;

@Context
public class AuditBehaviour extends AbstractAuditBehaviour {
  public AuditBehaviour(BehaviourManager manager) {
    manager.registerBehaviour(Model.class, Layer.SERVICE, List.of(When.AFTER), List.of(Action.CREATE, Action.UPDATE, Action.DELETE), 1000, this);
  }
}
