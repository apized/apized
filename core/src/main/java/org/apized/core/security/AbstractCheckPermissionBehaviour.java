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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.type.Argument;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import org.apized.core.MapHelper;
import org.apized.core.StringHelper;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.context.ApizedContext;
import org.apized.core.error.exception.ForbiddenException;
import org.apized.core.execution.Execution;
import org.apized.core.model.*;
import org.apized.core.mvc.ModelRepository;
import org.apized.core.security.annotation.Owner;
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
  public void process(Class<Model> type, When when, Action action, Execution<Model> execution) {
    Model model = execution.getInput();
    String modelId = execution.getId() != null ? execution.getId().toString() : null;
    String entityName = StringHelper.uncapitalize(type.getSimpleName());

    String fullPerm = slug + "." + entityName + "." + action.getType() + (modelId != null ? "." + modelId : "");
    boolean allowed = ApizedContext.getSecurity().getUser().isAllowed(fullPerm);

    //this might be simplified if we evaluate get actions after the action instead of before. as it stands we might get the model from the database twice in order to validate if the user can or cannot do the action
    if (!allowed && BeanIntrospection.getIntrospection(type).getBeanProperties().stream().anyMatch(prop -> prop.hasAnnotation(Owner.class))) {
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(
        Optional.ofNullable(model)
          .orElse(
            (Model) findBean(Argument.of(ModelRepository.class, type)).get().get(UUID.fromString(modelId)).get()//todo modelId is null when the action is list!!! Also the create action will probably also error out and additionally shouldn't be considered since just because something has an owner it's not reasonable to assume that a particular user is allowed to create it.
          )
      );
      Optional<BeanProperty<Model, Object>> ownerProp = BeanIntrospection.getIntrospection(type).getBeanProperties().stream()
        .filter(property -> property.hasAnnotation(Owner.class))
        .findFirst();
      if (ownerProp.isPresent() && ownerProp.get().getAnnotation(Owner.class).get("actions", Argument.of(List.class, Action.class)).get().contains(action)) {
        BeanProperty<Model, Object> prop = ownerProp.get();
        UUID ownerId = null;

        if (UUID.class.isAssignableFrom(prop.getType())) {
          ownerId = wrapper.getProperty(prop.getName(), UUID.class).orElse(null);
        } else if (Model.class.isAssignableFrom(prop.getType())) {
          ownerId = wrapper.getProperty(prop.getName(), Model.class).orElse(new DummyModel()).getId();
        }

        if (ApizedContext.getSecurity().getUser().getId().equals(ownerId) && !ApizedContext.getSecurity().getUser().getPermissions().stream().anyMatch(p -> p.startsWith(slug + "." + entityName + "." + action.getType() + "." + modelId))) {
          ApizedContext.getSecurity().getUser().getInferredPermissions().add(
            slug + "." + entityName + "." + action.getType() + "." + modelId
          );
          allowed = ApizedContext.getSecurity().getUser().isAllowed(fullPerm);
        }
      }
    }

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
          if (!ApizedContext.getSecurity().getUser().isAllowed(fieldPerm)) {
            Map<String, Object> originalFlatMap = MapHelper.flatten(BeanWrapper.getWrapper(model._getModelMetadata().getOriginal()).getProperty(field, Map.class).orElse(Map.of()), List.of(field));
            Map<String, Object> flatMap = MapHelper.flatten((Map<String, Object>) value, List.of(field));

            Set<String> keys = new HashSet<>(flatMap.keySet());
            keys.addAll(originalFlatMap.keySet());

            for (String key : keys) {
              if (!Objects.equals(originalFlatMap.get(key), flatMap.get(key))) {
                verifyPermissionForFieldAndValue(entityName, action, model, key, flatMap.get(key));
              }
            }
          }
        } else {
          verifyPermissionForFieldAndValue(entityName, action, model, field, value);
        }
      }
    }

    if (!allowed) {
      throw new ForbiddenException("Not allowed to " + action.getType() + " " + StringHelper.capitalize(entityName) + (modelId != null ? " with id " + modelId : ""), fullPerm);
    }
  }

  private void verifyPermissionForFieldAndValue(String entityName, Action action, Model model, String field, Object value) {
    Object val = value instanceof Model && ((Model) value).getId() != null ? ((Model) value).getId() : value;
    String perm = slug + "." + entityName + "." + action.getType() + (model.getId() != null ? "." + model.getId() : "") + "." + field + "." + val;
    if (!ApizedContext.getSecurity().getUser().isAllowed(perm)) {
      throw new ForbiddenException("Not allowed to " + action.getType() + " " + StringHelper.capitalize(entityName) + (model.getId() != null ? " with id " + model.getId() : "") + " with `" + field + "` set to " + val, perm);
    }
  }

  public abstract <K> Optional<K> findBean(Argument<K> argument);
}

