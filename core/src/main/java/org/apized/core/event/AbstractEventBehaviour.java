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

import org.apized.core.ApizedConfig;
import org.apized.core.ModelMapper;
import org.apized.core.ScopeHelper;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.context.ApizedContext;
import org.apized.core.event.annotation.EventField;
import org.apized.core.event.annotation.EventIgnore;
import org.apized.core.event.model.Event;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Model;
import org.apized.core.model.When;

public abstract class AbstractEventBehaviour implements BehaviourHandler<Model> {
  private final ModelMapper modelMapper = new ModelMapper(EventField.class, EventIgnore.class);

  @Override
  public void process(Class<Model> type, When when, Action action, Execution<Model> execution) {
    Model model = execution.getOutput();
    ScopeHelper.scopeUpUntil(
      model,
      a -> a.booleanValue("event").orElse(true),
      i -> ApizedContext.getEvent().add(
        new Event(
          ApizedContext.getRequest().getId(),
          String.format(
            "%s.%s.%sd",
            ApizedConfig.getInstance().getSlug(),
            i.getClass().getSimpleName().replaceAll("\\$Proxy$", ""),
            (type.isAssignableFrom(i.getClass()) ? action.getType() : Action.UPDATE)
          ).toLowerCase(),
          modelMapper.createMapOf(model)
        )
      )
    );
  }
}
