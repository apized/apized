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

package org.apized.core.behaviour;

import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.model.When;
import org.apized.core.tracing.Traced;

import java.util.UUID;

public interface BehaviourHandler<T extends Model> {
  @Traced(
    attributes = {
      @Traced.Attribute(key = "enricher.when", arg = "when"),
      @Traced.Attribute(key = "enricher.action", arg = "action"),
      @Traced.Attribute(key = "enricher.type", arg = "type")
    }
  )
  default void process(Class<T> type, When when, Action action, Execution<T> execution) {
    if (when == When.BEFORE) {
      switch (action) {
        case CREATE -> preCreate(execution, execution.getInput());
        case LIST -> preList(execution);
        case GET -> preGet(execution, execution.getId());
        case UPDATE -> preUpdate(execution, execution.getId(), execution.getInput());
        case DELETE -> preDelete(execution, execution.getId());
      }
    } else {
      switch (action) {
        case CREATE -> postCreate(execution, execution.getInput(), execution.getOutput());
        case LIST -> postList(execution, (Page<T>) execution.getOutput());
        case GET -> postGet(execution, execution.getId(), execution.getOutput());
        case UPDATE -> postUpdate(execution, execution.getId(), execution.getInput(), execution.getOutput());
        case DELETE -> postDelete(execution, execution.getId(), execution.getOutput());
      }
    }
  }

  default void preCreate(Execution execution, T input) {
  }

  default void postCreate(Execution execution, T input, T output) {
  }

  default void preList(Execution execution) {
  }

  default void postList(Execution execution, Page<T> output) {
  }

  default void preGet(Execution execution, UUID id) {
  }

  default void postGet(Execution execution, UUID id, T output) {
  }

  default void preUpdate(Execution execution, UUID id, T input) {
  }

  default void postUpdate(Execution execution, UUID id, T input, T output) {
  }

  default void preDelete(Execution execution, UUID id) {
  }

  default void postDelete(Execution execution, UUID id, T output) {
  }
}
