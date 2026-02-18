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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.type.Argument;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.core.error.exception.NotFoundException;
import org.apized.core.federation.Federation;
import org.apized.core.model.Action;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.apized.core.tracing.Traced;

import java.util.*;

@Slf4j
public abstract class AbstractModelService<T extends Model> implements ModelService<T> {
  public abstract Class<T> getType();

  protected abstract ModelRepository<T> getRepository();

  @Traced
  @Override
  public Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    return getRepository().list(page, pageSize, search, sort, skipAutoFilters);
  }

  @Traced
  @Override
  public Optional<T> searchOne(List<SearchTerm> search) {
    Optional<T> t = getRepository().searchOne(search);
    t.ifPresent(value -> ApizedContext.getRequest().getPathVariables().put(
      StringHelper.uncapitalize(getType().getSimpleName()),
      value.getId()
    ));
    return t;
  }

  @Traced
  @Override
  public Page<T> list(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    return getRepository().search(search, sort, skipAutoFilters);
  }

  @Traced
  @Override
  public T find(UUID id) {
    Optional<T> t = getRepository().get(id);
    t.ifPresent(value -> ApizedContext.getRequest().getPathVariables().put(
      StringHelper.uncapitalize(getType().getSimpleName()),
      value.getId()
    ));
    return t.orElseThrow(NotFoundException::new);
  }

  @Traced
  @Override
  public T get(UUID id) {
    Optional<T> t = getRepository().get(id);
    t.ifPresent(value -> ApizedContext.getRequest().getPathVariables().put(
      StringHelper.uncapitalize(getType().getSimpleName()),
      value.getId()
    ));
    return t.orElseThrow(NotFoundException::new);
  }

  @SuppressWarnings("unchecked")
  @Traced
  @Override
  public T create(T it) {
    ExecutionPlan plan = new ExecutionPlan();
    planExecutions(plan, Action.CREATE, it, true);
    plan.main((AbstractModelService<Model>) this, it, "", (model) -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PrePersist.class) != null)
        .forEach(prePersist -> prePersist.invoke((T) model));

      T create = getRepository().create((T) model);
      ApizedContext.getRequest().getPathVariables().put(StringHelper.uncapitalize(getType().getSimpleName()), create.getId());

      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PostPersist.class) != null)
        .forEach(postPersist -> postPersist.invoke((T) model));

      return create;
    });
    planExecutions(plan, Action.CREATE, it, false);
    log.debug("Plan: Create {} {}", getType().getSimpleName(), plan);
    return (T) plan.execute().getResponse();
  }

  @Traced
  @Override
  public List<T> batchCreate(List<T> it) {
    if (it.isEmpty()) return it;

    it.forEach(i -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PrePersist.class) != null)
        .forEach(prePersist -> prePersist.invoke(i));
    });

    List<T> create = getRepository().batchCreate(it);

    it.forEach(i -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PostPersist.class) != null)
        .forEach(postPersist -> postPersist.invoke(i));
    });

    return create;
  }

  @SuppressWarnings("unchecked")
  @Traced
  @Override
  public T update(UUID id, T it) {
    ExecutionPlan plan = new ExecutionPlan();
    planExecutions(plan, Action.UPDATE, it, true);
    plan.main((AbstractModelService<Model>) this, it, "", (model) -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PreUpdate.class) != null)
        .forEach(preUpdate -> preUpdate.invoke((T) model));

      T update = getRepository().update(id, (T) model);

      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PostUpdate.class) != null)
        .forEach(postUpdate -> postUpdate.invoke((T) model));

      return update;
    });
    planExecutions(plan, Action.UPDATE, it, false);
    log.debug("Plan: Update {} {}", getType().getSimpleName(), plan);
    return (T) plan.execute().getResponse();
  }

  @Traced
  @Override
  public List<T> batchUpdate(List<T> it) {
    if (it.isEmpty()) return it;

    it.forEach(i -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PreUpdate.class) != null)
        .forEach(preUpdate -> preUpdate.invoke(i));
    });

    List<T> updates = getRepository().batchUpdate(it);

    it.forEach(i -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PostUpdate.class) != null)
        .forEach(postUpdate -> postUpdate.invoke(i));
    });

    return updates;
  }

  @SuppressWarnings("unchecked")
  @Traced
  @Override
  public T delete(UUID id) {
    T it = get(id);
    markForDeletion(getType(), it);
    ExecutionPlan plan = new ExecutionPlan();
    planExecutions(plan, Action.DELETE, it, true);
    plan.main((AbstractModelService<Model>) this, it, "", (model) -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PreRemove.class) != null)
        .forEach(preRemove -> preRemove.invoke((T) model));

      getRepository().delete(model.getId());

      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PostRemove.class) != null)
        .forEach(postRemove -> postRemove.invoke((T) model));

      return model;
    });
    planExecutions(plan, Action.DELETE, it, false);
    log.debug("Plan: Delete {} {}", getType().getSimpleName(), plan);
    return (T) plan.execute().getResponse();
  }

  @Traced
  @Override
  public List<T> batchDelete(List<T> it) {
    if (it.isEmpty()) return it;

    it.forEach(i -> {
      i._getModelMetadata().setAction(Action.DELETE);

      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PreRemove.class) != null)
        .forEach(preRemove -> preRemove.invoke(i));
    });

    getRepository().batchDelete(it.stream().map(Model::getId).toList());

    it.forEach(i -> {
      BeanIntrospection
        .getIntrospection(getType())
        .getBeanMethods()
        .stream()
        .filter(m -> m.getAnnotation(PostRemove.class) != null)
        .forEach(postRemove -> postRemove.invoke(i));
    });
    return it;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void planExecutions(ExecutionPlan plan, Action action, T model, boolean isBefore) {
    if (plan.isProcessed(model, isBefore)) {
      return;
    }
    plan.markProcessed(model, isBefore);

    BeanIntrospection<T> introspection = BeanIntrospection.getIntrospection(getType());

    List<BeanProperty<T, Object>> properties = introspection.getBeanProperties().stream()
      .filter(p -> !p.hasAnnotation(Federation.class))
      .filter(p -> Model.class.isAssignableFrom(p.getType()) || (Collection.class.isAssignableFrom(p.getType()) && Model.class.isAssignableFrom(p.asArgument().getTypeParameters()[0].getType())))
      .filter(p -> model._getModelMetadata().getTouched().contains(p.getName()))
      .toList();
    for (BeanProperty<T, Object> property : properties) {
      BeanWrapper<T> wrapper = BeanWrapper.getWrapper(model);
      Optional<Object> value = wrapper.getProperty(property.getName(), property.getType());
      List<Model> values = new ArrayList<>();

      Class<Object> type = property.getType();
      if (value.isPresent()) {
        if (Collection.class.isAssignableFrom(type)) {
          type = property.asArgument().getTypeParameters()[0].getType();
          values = (List<Model>) value.get();
        } else {
          values.add((Model) value.get());
        }
      }

      Optional<ModelService> service = findBean(Argument.of(ModelService.class, type));
      if (service.isEmpty()) continue;

      final AbstractModelService<Model> modelService = (AbstractModelService<Model>) service.get();

      switch (action) {
        case CREATE -> {
          if (isBefore) {
            planPreCreate(plan, model, property, modelService, values, type);
          } else {
            planPostCreate(plan, model, property, modelService, values, type);
          }
        }
        case UPDATE -> {
          if (isBefore) {
            planPreUpdate(plan, model, property, modelService, values, type);
          } else {
            planPostUpdate(plan, model, property, modelService, values, type);
          }
        }
        case DELETE -> {
          if (isBefore) {
            planPreDelete(plan, model, property, modelService, values, type);
          } else {
            planPostDelete(plan, model, property, modelService, values, type);
          }
        }
        default -> throw new IllegalStateException("Unhandled action: " + action);
      }
    }
  }

  private void planPreCreate(ExecutionPlan plan, T model, BeanProperty<T, Object> property, AbstractModelService<Model> service, List<Model> values, Class<Object> type) {
    if (property.hasAnnotation(OneToOne.class) && property.getAnnotation(OneToOne.class).stringValue("mappedBy").isEmpty()) {
      log.trace("CREATE[{}][OneToOne][PRE] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, true);
      processAction(plan, property, values, service, Action.UPDATE, true);
    } else if (property.hasAnnotation(ManyToOne.class)) {
      log.trace("CREATE[{}][ManyToOne][PRE] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, true);
      processAction(plan, property, values, service, Action.UPDATE, true);
    } else if (property.hasAnnotation(ManyToMany.class) && property.getAnnotation(ManyToMany.class).stringValue("mappedBy").isPresent()) {
      log.trace("CREATE[{}][ManyToMany][PRE] - {}", getType().getSimpleName(), property.getName());
    }
  }

  private void planPostCreate(ExecutionPlan plan, T model, BeanProperty<T, Object> property, AbstractModelService<Model> service, List<Model> values, Class<Object> type) {
    if (property.hasAnnotation(OneToOne.class) && property.getAnnotation(OneToOne.class).stringValue("mappedBy").isPresent()) {
      log.trace("CREATE[{}][OneToOne][POST] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, false);
      processAction(plan, property, values, service, Action.UPDATE, false);
    } else if (property.hasAnnotation(OneToMany.class)) {
      log.trace("CREATE[{}][OneToMany][POST] - {}", getType().getSimpleName(), property.getName());
      updateOneToManyValues(model, property, values, type);
      processAction(plan, property, values, service, Action.CREATE, false);
      processAction(plan, property, values, service, Action.UPDATE, false);
      processAction(plan, property, values, service, Action.DELETE, false);
    } else if (property.hasAnnotation(ManyToMany.class) && property.getAnnotation(ManyToMany.class).stringValue("mappedBy").isEmpty()) {
      log.trace("CREATE[{}][ManyToMany][POST] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, false);
      processManyToMany(plan, model, property, values, service);
      processAction(plan, property, values, service, Action.UPDATE, false);
    }
  }

  private void planPreUpdate(ExecutionPlan plan, T model, BeanProperty<T, Object> property, AbstractModelService<Model> service, List<Model> values, Class<Object> type) {
    if (property.hasAnnotation(OneToOne.class) && property.getAnnotation(OneToOne.class).stringValue("mappedBy").isEmpty()) {
      log.trace("UPDATE[{}][OneToOne][PRE] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, true);
      processAction(plan, property, values, service, Action.UPDATE, true);
    } else if (property.hasAnnotation(ManyToOne.class)) {
      log.trace("UPDATE[{}][ManyToOne][PRE] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, true);
      processAction(plan, property, values, service, Action.UPDATE, true);
    } else if (property.hasAnnotation(ManyToMany.class) && property.getAnnotation(ManyToMany.class).stringValue("mappedBy").isPresent()) {
      log.trace("UPDATE[{}][ManyToMany][PRE] - {}", getType().getSimpleName(), property.getName());
    }
  }

  private void planPostUpdate(ExecutionPlan plan, T model, BeanProperty<T, Object> property, AbstractModelService<Model> service, List<Model> values, Class<Object> type) {
    if (property.hasAnnotation(OneToOne.class) && property.getAnnotation(OneToOne.class).stringValue("mappedBy").isPresent()) {
      log.trace("UPDATE[{}][OneToOne][POST] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, false);
      processAction(plan, property, values, service, Action.UPDATE, false);
    } else if (property.hasAnnotation(OneToMany.class)) {
      log.trace("UPDATE[{}][OneToMany][POST] - {}", getType().getSimpleName(), property.getName());
      updateOneToManyValues(model, property, values, type);
      processAction(plan, property, values, service, Action.CREATE, false);
      processAction(plan, property, values, service, Action.UPDATE, false);
      processAction(plan, property, values, service, Action.DELETE, false);
    } else if (property.hasAnnotation(ManyToMany.class) && property.getAnnotation(ManyToMany.class).stringValue("mappedBy").isEmpty()) {
      log.trace("UPDATE[{}][ManyToMany][POST] - {}", getType().getSimpleName(), property.getName());
      processAction(plan, property, values, service, Action.CREATE, false);
      processManyToMany(plan, model, property, values, service);
      processAction(plan, property, values, service, Action.UPDATE, false);
    }
  }

  private void planPreDelete(ExecutionPlan plan, T model, BeanProperty<T, Object> property, AbstractModelService<Model> service, List<Model> values, Class<Object> type) {
    if (property.hasAnnotation(OneToOne.class) && property.getAnnotation(OneToOne.class).stringValue("mappedBy").isPresent()) {
      log.trace("DELETE[{}][OneToOne][PRE] - {}", getType().getSimpleName(), property.getName());
    } else if (property.hasAnnotation(OneToMany.class)) {
      log.trace("DELETE[{}][OneToMany][PRE] - {}", getType().getSimpleName(), property.getName());
      if (property.getAnnotation(OneToMany.class).booleanValue("orphanRemoval").orElse(false)) {
        values.forEach(value -> markForDeletion(type, value));
      }
      processAction(plan, property, values, service, Action.DELETE, true);
    } else if (property.hasAnnotation(ManyToMany.class) && property.getAnnotation(ManyToMany.class).stringValue("mappedBy").isEmpty()) {
      log.trace("DELETE[{}][ManyToMany][PRE] - {}", getType().getSimpleName(), property.getName());
      processManyToMany(plan, model, property, values, service);
      processAction(plan, property, values, service, Action.DELETE, false);
    }
  }

  private void planPostDelete(ExecutionPlan plan, T model, BeanProperty<T, Object> property, AbstractModelService<Model> service, List<Model> values, Class<Object> type) {
    if (property.hasAnnotation(OneToOne.class) && property.getAnnotation(OneToOne.class).stringValue("mappedBy").isPresent()) {
      log.trace("DELETE[{}][OneToOne][POST] - {}", getType().getSimpleName(), property.getName());
    } else if (property.hasAnnotation(OneToMany.class)) {
      log.trace("DELETE[{}][OneToMany][POST] - {}", getType().getSimpleName(), property.getName());
    } else if (property.hasAnnotation(ManyToMany.class) && property.getAnnotation(ManyToMany.class).stringValue("mappedBy").isEmpty()) {
      log.trace("DELETE[{}][ManyToMany][POST] - {}", getType().getSimpleName(), property.getName());
    }
  }

  private void markForDeletion(Class<?> type, Model model) {
    model._getModelMetadata().setAction(Action.DELETE);
    model._getModelMetadata().getTouched().addAll(
      BeanIntrospection.getIntrospection(type).getBeanProperties().stream()
        .filter(p -> p.hasAnnotation(OneToOne.class) || p.hasAnnotation(OneToMany.class) || p.hasAnnotation(ManyToOne.class) || p.hasAnnotation(ManyToMany.class))
        .map(BeanProperty::getName)
        .toList()
    );
  }

  private void processAction(ExecutionPlan plan, BeanProperty<T, Object> p, List<Model> values, AbstractModelService<Model> service, Action action, boolean isBefore) {
    List<Model> models = values.stream()
      .filter(m -> m._getModelMetadata().isDirty() && m._getModelMetadata().getAction().equals(action))
      .filter(m -> !plan.isProcessed(m, isBefore))
      .toList();
    if (!models.isEmpty()) {
      models.forEach((model) -> service.planExecutions(plan, action, model, true));
      switch (action) {
        case CREATE -> plan.batchCreate(service, models, p.getName());
        case UPDATE -> plan.batchUpdate(service, models, p.getName());
        case DELETE -> plan.batchDelete(service, models, p.getName());
        default -> throw new IllegalStateException("Unexpected action: " + action);
      }
      models.forEach((model) -> service.planExecutions(plan, action, model, false));
    }
  }

  @SuppressWarnings({"unchecked", "DuplicatedCode"})
  private void processManyToMany(ExecutionPlan plan, T it, BeanProperty<T, Object> property, List<Model> values, AbstractModelService<Model> service) {
    Model original = it._getModelMetadata().getOriginal();
    List<Model> remove = new ArrayList<>();
    List<Model> add = new ArrayList<>(values.stream().toList());

    if (original != null) {
      BeanWrapper<?> originalWrapper = BeanWrapper.getWrapper(original);
      if (it._getModelMetadata().getAction().equals(Action.DELETE)) {
        add.clear();
        remove.addAll(values);
      } else {
        ((List<Model>) originalWrapper.getProperty(property.getName(), property.getType()).get()).forEach(o -> {
          if (add.stream().anyMatch(model -> o.getId().equals(model.getId()))) {
            add.removeIf(model -> o.getId().equals(model.getId()));
          } else {
            remove.add(o);
          }
        });
      }
    }
    add.forEach(o -> plan.addManyToMany((AbstractModelService<Model>) this, it, property.getName(), o));
    remove.forEach(o -> {
      plan.removeManyToMany((AbstractModelService<Model>) this, it, property.getName(), o);
    });
    if (!remove.isEmpty() && List.of(CascadeType.ALL, CascadeType.REMOVE).contains(property.getAnnotation(ManyToMany.class).enumValue("cascade", CascadeType.class).orElse(CascadeType.DETACH))) {
      plan.batchDelete(service, remove, property.getName());
    }
  }

  @SuppressWarnings({"unchecked", "DuplicatedCode"})
  private void updateOneToManyValues(T model, BeanProperty<T, Object> property, List<Model> values, Class<Object> type) {
    Model original = model._getModelMetadata().getOriginal();
    List<Model> remove = new ArrayList<>();
    List<Model> add = new ArrayList<>(values.stream().toList());

    if (original != null) {
      BeanWrapper<?> originalWrapper = BeanWrapper.getWrapper(original);
      if (model._getModelMetadata().getAction().equals(Action.DELETE)) {
        add.clear();
        remove.addAll(values);
      } else {
        ((List<Model>) originalWrapper.getProperty(property.getName(), property.getType()).get()).forEach(o -> {
          if (add.stream().anyMatch(m -> o.getId().equals(m.getId()))) {
            add.remove(o);
          } else {
            remove.add(o);
          }
        });
      }
    }

    AnnotationValue<OneToMany> annotation = property.getAnnotation(OneToMany.class);
    String field = annotation.stringValue("mappedBy").orElse(StringHelper.uncapitalize(getType().getSimpleName()));
    add.forEach(o -> {
      BeanWrapper.getWrapper(o).setProperty(field, model);
      if (o._getModelMetadata().getAction().equals(Action.NO_OP)) {
        o._getModelMetadata().setAction(Action.UPDATE);
      }
    });
    remove.forEach(o -> {
      BeanWrapper.getWrapper(o).setProperty(field, null);
      if (annotation.booleanValue("orphanRemoval").orElse(false)) {
        markForDeletion(type, o);
      } else if (o._getModelMetadata().getAction().equals(Action.NO_OP)) {
        o._getModelMetadata().setAction(Action.UPDATE);
      }
    });
    values.clear();
    values.addAll(add);
    values.addAll(remove);
  }

  public abstract <K> Optional<K> findBean(Argument<K> argument);
}
