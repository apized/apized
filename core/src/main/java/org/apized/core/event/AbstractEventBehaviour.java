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

package org.apized.core.event;

import org.apized.core.ModelMapper;
import org.apized.core.ScopeHelper;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.event.annotation.EventField;
import org.apized.core.event.annotation.EventIgnore;
import org.apized.core.event.model.Event;
import org.apized.core.execution.Execution;
import org.apized.core.model.*;
import org.apized.core.serde.RequestContext;

import java.util.List;

public abstract class AbstractEventBehaviour implements BehaviourHandler<Model> {
  private final ModelMapper modelMapper;

  public AbstractEventBehaviour(BehaviourManager manager) {
    manager.registerBehaviour(
      Model.class,
      Layer.CONTROLLER,
      List.of(When.AFTER),
      List.of(Action.CREATE, Action.UPDATE, Action.DELETE),
      1000,
      this
    );

    modelMapper = new ModelMapper(EventField.class, EventIgnore.class);
  }

  @Override
  public void process(Class<Model> type, When when, Action action, Execution execution) {
    Model model = (Model) execution.getInputs().get("it");
    ScopeHelper.scopeUpUntil(
      model,
      a -> a.booleanValue("event").orElse(false),
      i -> EventContext.getInstance().add(new Event(
        RequestContext.getInstance().getId(),
        String.format("%s.%s.%s", "micronaut", type.getSimpleName().toLowerCase(), type.equals(i.getClass()) ? action.getType() : Action.UPDATE),
        modelMapper.createMapOf(model)
      ))
    );
  }
}
