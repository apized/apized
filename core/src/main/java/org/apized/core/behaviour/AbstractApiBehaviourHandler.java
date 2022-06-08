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

import java.util.UUID;

public abstract class AbstractApiBehaviourHandler<T extends Model> implements BehaviourHandler<T> {
  @Override
  public void process(Class<T> type, When when, Action action, Execution execution) {
    if (when == When.BEFORE) {
      switch (action) {
        case CREATE -> preCreate(execution, (T) execution.getInputs().get("it"));
        case LIST -> preList(execution);
        case GET -> preGet(execution, (UUID) execution.getInputs().get("id"));
        case UPDATE ->
          preUpdate(execution, (UUID) execution.getInputs().get("id"), (T) execution.getInputs().get("it"));
        case DELETE -> preDelete(execution, (UUID) execution.getInputs().get("id"));
      }
    } else {
      switch (action) {
        case CREATE -> postCreate(execution, (T) execution.getInputs().get("it"), (T) execution.getOutput());
        case LIST -> postList(execution, (Page<T>) execution.getOutput());
        case GET -> postGet(execution, (UUID) execution.getInputs().get("id"), (T) execution.getOutput());
        case UPDATE ->
          postUpdate(execution, (UUID) execution.getInputs().get("id"), (T) execution.getInputs().get("it"), (T) execution.getOutput());
        case DELETE -> postDelete(execution, (UUID) execution.getInputs().get("id"), (T) execution.getOutput());
      }
    }
  }

  public void preCreate(Execution execution, T input) { }

  public void postCreate(Execution execution, T input, T output) { }

  public void preList(Execution execution) { }

  public void postList(Execution execution, Page<T> output) { }

  public void preGet(Execution execution, UUID id) { }

  public void postGet(Execution execution, UUID id, T output) { }

  public void preUpdate(Execution execution, UUID id, T it) { }

  public void postUpdate(Execution execution, UUID id, T output, T executionOutput) { }

  public void preDelete(Execution execution, UUID id) { }

  public void postDelete(Execution execution, UUID id, T output) { }
}
