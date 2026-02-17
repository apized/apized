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
import org.apized.core.model.Apized;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.apized.core.tracing.Traced;

import java.util.*;
import java.util.stream.Stream;

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
    planExecutions(plan, it, true);
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
    planExecutions(plan, it, false);
    log.debug("Create plan:{}", plan);
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
    planExecutions(plan, it, true);
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
    planExecutions(plan, it, false);
    log.debug("Update plan:{}", plan);
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
    it._getModelMetadata().setAction(Action.DELETE);
    ExecutionPlan plan = new ExecutionPlan();
    planExecutions(plan, it, true);
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
    planExecutions(plan, it, false);
    log.debug("Delete plan:{}", plan);
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

  @SuppressWarnings({"unchecked", "rawtypes", "OptionalGetWithoutIsPresent"})
  public void planExecutions(ExecutionPlan plan, T it, boolean isBefore) {
    if (plan.isProcessed(it, isBefore)) {
      return;
    }
    BeanWrapper<T> wrapper = BeanWrapper.getWrapper(it);
    BeanIntrospection<T> introspection = BeanIntrospection.getIntrospection(getType());

    List<BeanProperty<T, Object>> properties = introspection.getBeanProperties().stream()
      .filter(p -> !p.hasAnnotation(Federation.class))
      .filter(p -> it._getModelMetadata().getTouched().contains(p.getName()) || (p.hasAnnotation(OneToMany.class) && it._getModelMetadata().getAction().equals(Action.DELETE)))
      .filter(p -> Model.class.isAssignableFrom(p.getType()) || (Collection.class.isAssignableFrom(p.getType()) && Model.class.isAssignableFrom(p.asArgument().getTypeParameters()[0].getType())))
      .filter(p -> isBefore == (
          (p.hasAnnotation(OneToOne.class) && p.getAnnotation(OneToOne.class).stringValue("mappedBy").isEmpty())
            || p.hasAnnotation(ManyToOne.class)
            || (p.hasAnnotation(OneToMany.class) && it._getModelMetadata().getAction().equals(Action.DELETE))
            || (p.hasAnnotation(ManyToMany.class) && p.getAnnotation(ManyToMany.class).stringValue("mappedBy").isPresent())
        )
      ).toList();

    List<Model> mToMAdd = new ArrayList<>();
    List<Model> mToMRemove = new ArrayList<>();

    properties.forEach(p -> {
      Optional<Object> value = wrapper.getProperty(p.getName(), p.getType());
      List<Model> values = new ArrayList<>();

      if (value.isPresent()) {
        Class<Object> type = p.getType();

        if (Collection.class.isAssignableFrom(type)) {
          type = p.asArgument().getTypeParameters()[0].getType();
          values = (List<Model>) value.get();
        } else {
          values.add((Model) value.get());
        }

        Optional<ModelService> service = findBean(
          Argument.of(
            ModelService.class,
            type
          )
        );

        if (p.hasAnnotation(ManyToMany.class) || p.hasAnnotation(OneToMany.class)) {
          Model original = it._getModelMetadata().getOriginal();
          List<Model> remove = new ArrayList<>();
          List<Model> add = new ArrayList<>(values.stream().toList());

          if (original != null) {
            BeanWrapper<?> originalWrapper = BeanWrapper.getWrapper(original);
            if (it._getModelMetadata().getAction().equals(Action.DELETE)) {
              add.clear();
              remove.addAll(values);
            } else {
              ((List<Model>) originalWrapper.getProperty(p.getName(), p.getType()).get()).forEach(o -> {
                if (add.stream().anyMatch(model -> o.getId().equals(model.getId()))) {
                  add.remove(o);
                } else {
                  remove.add(o);
                }
              });
            }
          }

          if (p.hasAnnotation(ManyToMany.class)) {
            mToMAdd.addAll(add);
            mToMRemove.addAll(remove);
          } else if (p.hasAnnotation(OneToMany.class)) {
            AnnotationValue<OneToMany> annotation = p.getAnnotation(OneToMany.class);
            String field = annotation.stringValue("mappedBy").orElse(StringHelper.uncapitalize(getType().getSimpleName()));
            add.forEach(o -> {
              BeanWrapper.getWrapper(o).setProperty(field, it);
              if (o._getModelMetadata().getAction().equals(Action.NO_OP)) {
                o._getModelMetadata().setAction(Action.UPDATE);
              }
            });
            remove.forEach(o -> {
              BeanWrapper.getWrapper(o).setProperty(field, null);
              if (annotation.booleanValue("orphanRemoval").orElse(false)) {
                o._getModelMetadata().setAction(Action.DELETE);
              } else if (o._getModelMetadata().getAction().equals(Action.NO_OP)) {
                o._getModelMetadata().setAction(Action.UPDATE);
              }
            });
            values = new ArrayList<>();
            values.addAll(add);
            values.addAll(remove);
          }
        }

        if (!it._getModelMetadata().getAction().equals(Action.DELETE) && p.hasAnnotation(OneToMany.class) && !List.of(BeanIntrospection.getIntrospection(type).getAnnotation(Apized.class).classValues("scope")).contains(getType())) {
          String field = p.getAnnotation(OneToMany.class).stringValue("mappedBy").orElse(StringHelper.uncapitalize(getType().getSimpleName()));
          values.forEach(subModel -> {
            BeanWrapper.getWrapper(subModel).setProperty(field, it);
            if (subModel._getModelMetadata().getAction().equals(Action.NO_OP)) {
              subModel._getModelMetadata().setAction(Action.UPDATE);
            }
          });
        }

        if (service.isPresent()) {
//            values.stream()
//              .filter(m -> m._getModelMetadata().isDirty())
//              .forEach(subModel -> {
//                UUID subModelId = subModel.getId();
//                switch (subModel._getModelMetadata().getAction()) {
//                  case CREATE -> service.get().create(subModel);
//                  case UPDATE -> service.get().update(subModelId, subModel);
//                  case DELETE ->
//                    service.get().delete(subModelId);//todo should this have the reverse order of create/update?
//                }
//              });

          List<Model> creates = values.stream()
            .filter(m -> m._getModelMetadata().isDirty() && m._getModelMetadata().getAction().equals(Action.CREATE))
            .filter(m -> !plan.isProcessed(m, isBefore))
            .toList();
          if (!creates.isEmpty()) {
            creates.forEach((create) -> service.get().planExecutions(plan, create, true));
            plan.batchCreate((AbstractModelService<Model>) service.get(), creates, p.getName());
            creates.forEach((create) -> service.get().planExecutions(plan, create, false));
          }

          mToMAdd.forEach(o -> plan.addManyToMany((AbstractModelService<Model>) this, it, p.getName(), o));
          mToMRemove.forEach(o -> plan.removeManyToMany((AbstractModelService<Model>) this, it, p.getName(), o));

          List<Model> updates = values.stream()
            .filter(m -> m._getModelMetadata().isDirty() && m._getModelMetadata().getAction().equals(Action.UPDATE))
            .filter(m -> !plan.isProcessed(m, isBefore))
            .toList();
          if (!updates.isEmpty()) {
            updates.forEach((update) -> service.get().planExecutions(plan, update, true));
            plan.batchUpdate((AbstractModelService<Model>) service.get(), updates, p.getName());
            updates.forEach((update) -> service.get().planExecutions(plan, update, false));
          }

          List<Model> deletes = values.stream()
            .filter(m -> m._getModelMetadata().isDirty() && m._getModelMetadata().getAction().equals(Action.DELETE))
            .filter(m -> !plan.isProcessed(m, isBefore))
            .toList();
          if (!deletes.isEmpty()) {
            deletes.forEach((delete) -> service.get().planExecutions(plan, delete, true));
            plan.batchDelete((AbstractModelService<Model>) service.get(), deletes, p.getName());
            deletes.forEach((delete) -> service.get().planExecutions(plan, delete, false));
          }
        }
      }
    });
    plan.markProcessed(it, isBefore);
  }

  public abstract <K> Optional<K> findBean(Argument<K> argument);
}
