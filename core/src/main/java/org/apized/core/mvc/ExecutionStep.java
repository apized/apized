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

package org.apized.core.mvc;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apized.core.model.Model;

import java.util.*;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class ExecutionStep {
  public enum Kind {
    MAIN,
    CREATE,
    UPDATE,
    DELETE,
    ADD_MANY_TO_MANY,
    REMOVE_MANY_TO_MANY,
  }

//  public interface MainExecutor {
//    Model execute(ExecutionStep step);
//  }

  private Kind kind;
  private AbstractModelService<Model> modelService;
  private List<Model> models;
  private String propertyName;
  private Model other;
  private Consumer<ExecutionStep> consumer;

  public void execute() {
    consumer.accept(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("kind: ").append(kind);
    sb.append(", propertyName: '").append(propertyName).append("'");
    sb.append(", type: ").append(models.getFirst().getClass().getSuperclass().getSimpleName());
    sb.append(", models: ").append(models.stream().map((m) -> Map.of(Optional.ofNullable(m.getId()).orElse(UUID.fromString("00000000-0000-0000-0000-000000000000")), m._getModelMetadata().getTouched())).toList());
    Optional.ofNullable(other).ifPresent((model) -> sb.append(", other: ").append(model.getId()));
    sb.append("}");
    return sb.toString();
  }
}
