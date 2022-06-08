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

package org.apized.core.security;

import org.apized.core.MapHelper;
import org.apized.core.StringHelper;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.error.exception.ForbiddenException;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public abstract class AbstractCheckPermissionBehaviour implements BehaviourHandler<Model> {
  protected String slug;
  Logger log = LoggerFactory.getLogger(this.getClass());

  public AbstractCheckPermissionBehaviour(String slug, BehaviourManager manager) {
    this.slug = slug;
    manager.registerBehaviour(
      Model.class,
      Layer.SERVICE,
      List.of(When.BEFORE),
      List.of(Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE),
      -1000,
      this
    );
  }

  @Override
  public void process(Class<Model> type, When when, Action action, Execution execution) {
    Model model = (Model) execution.getInputs().get("it");
    String modelId = model != null && model.getId() != null ? model.getId().toString() : null;
    String entityName = StringHelper.uncapitalize(type.getSimpleName());

    String fullPerm = slug + "." + entityName + "." + action.getType() + (modelId != null ? "." + modelId : "");
    boolean allowed = SecurityContext.getInstance().getUser().isAllowed(fullPerm);

    log.debug("Permissions check for '" + fullPerm + "' " + (allowed ? "passed" : "failed"));

    if (!allowed && model != null && model._getModelMetadata().getTouched().size() > 0) {
      allowed = true;
      for (String field : model._getModelMetadata().getTouched()) {
        BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model);
        BeanProperty<Model, Object> property = wrapper.getIntrospection().getProperty(field).orElse(null);
        Optional<AnnotationValue<TypeDef>> dataType = Optional.ofNullable(property.getAnnotation(TypeDef.class));
        boolean isJsonb = dataType.isPresent() && dataType.get().enumValue("type", DataType.class).equals(DataType.JSON);

        Object value = wrapper.getProperty(field, property.getType()).orElse(null);

        if (value instanceof List) {
          for (Object val : (List<?>) value) {
            verifyPermissionForFieldAndValue(entityName, action, model, field, val);
          }
        } else if (isJsonb) {
          String fieldPerm = slug + "." + entityName + "." + action.getType() + (modelId != null ? "." + modelId : "") + "." + field;
          if (!SecurityContext.getInstance().getUser().isAllowed(fieldPerm)) {
            //todo ensure the original is here so we can compare it with the input
//            Map<String, Object> originalFlatMap = flatten((Map<String, Object>) ReflectionHelper.getProperty(execution.getOriginal(), field), List.of(field));
            Map<String, Object> flatMap = MapHelper.flatten((Map<String, Object>) value, List.of(field));

            Set<String> keys = new HashSet<>(flatMap.keySet());
//            keys.addAll(originalFlatMap.keySet());

            for (String key : keys) {
//              if (!Objects.equals(originalFlatMap.get(key), flatMap.get(key))) {
              verifyPermissionForFieldAndValue(entityName, action, model, key, flatMap.get(key));
//              }
            }
          }
        } else {
          verifyPermissionForFieldAndValue(entityName, action, model, field, value);
        }
      }
    }

    if (!allowed) {
      throw new ForbiddenException("Not allowed to " + action.getType() + " " + entityName + (model != null && model.getId() != null ? " with id " + model.getId() : ""), fullPerm);
    }
  }

  private void verifyPermissionForFieldAndValue(String entityName, Action action, Model model, String field, Object value) {
    String perm = slug + "." + entityName + "." + action.getType() + (model.getId() != null ? "." + model.getId() : "") + "." + field + "." + value;
    if (!SecurityContext.getInstance().getUser().isAllowed(perm)) {
      throw new ForbiddenException("Not allowed to " + action.getType() + " " + entityName + (model.getId() != null ? " with id " + model.getId() : "") + " with `" + field + "` set to " + value, perm);
    }
  }
}

