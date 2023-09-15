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

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.DefaultArgument;
import jakarta.persistence.*;
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

import java.util.*;

public abstract class AbstractModelService<T extends Model> implements ModelService<T> {
  ApplicationContext appContext;

  public abstract Class<T> getType();

  protected abstract ModelRepository<T> getRepository();

  public AbstractModelService(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort) {
    return getRepository().list(page, pageSize, search, sort);
  }

  @Override
  public Optional<T> searchOne(List<SearchTerm> search) {
    Optional<T> t = getRepository().searchOne(search);
    t.ifPresent(value -> ApizedContext.getRequest().getPathVariables().put(
      StringHelper.uncapitalize(getType().getSimpleName()),
      value.getId()
    ));
    return t;
  }

  @Override
  public Page<T> list(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    return getRepository().search(search, sort, skipAutoFilters);
  }

  @Override
  public T find(UUID id) {
    Optional<T> t = getRepository().get(id);
    t.ifPresent(value -> ApizedContext.getRequest().getPathVariables().put(
      StringHelper.uncapitalize(getType().getSimpleName()),
      value.getId()
    ));
    return t.orElseThrow(NotFoundException::new);
  }

  @Override
  public T get(UUID id) {
    Optional<T> t = getRepository().get(id);
    t.ifPresent(value -> ApizedContext.getRequest().getPathVariables().put(
      StringHelper.uncapitalize(getType().getSimpleName()),
      value.getId()
    ));
    return t.orElseThrow(NotFoundException::new);
  }

  @Override
  public T create(T it) {
    performSubExecutions(it, true);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PrePersist.class) != null)
      .forEach(prePersist -> prePersist.invoke(it));

    T create = getRepository().create(it);
    ApizedContext.getRequest().getPathVariables().put(StringHelper.uncapitalize(getType().getSimpleName()), create.getId());

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PostPersist.class) != null)
      .forEach(prePersist -> prePersist.invoke(it));

    performSubExecutions(it, false);
    return create;
  }

  @Override
  public T update(UUID id, T it) {
    it.setId(id);
    performSubExecutions(it, true);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PreUpdate.class) != null)
      .forEach(preUpdate -> preUpdate.invoke(it));

    T update = getRepository().update(id, it);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PostUpdate.class) != null)
      .forEach(preUpdate -> preUpdate.invoke(it));

    performSubExecutions(it, false);
    return update;
  }

  @Override
  public T delete(UUID id) {
    T it = get(id);
    it._getModelMetadata().setAction(Action.DELETE);
    performSubExecutions(it, true);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PreRemove.class) != null)
      .forEach(prePersist -> prePersist.invoke(it));

    getRepository().delete(id);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PostRemove.class) != null)
      .forEach(prePersist -> prePersist.invoke(it));

    performSubExecutions(it, false);
    return it;
  }

  @SuppressWarnings({"unchecked", "rawtypes", "DataFlowIssue", "OptionalGetWithoutIsPresent"})
  protected void performSubExecutions(T it, boolean isBefore) {
    BeanWrapper<T> wrapper = BeanWrapper.getWrapper(it);
    BeanIntrospection<T> introspection = BeanIntrospection.getIntrospection(getType());

    introspection.getBeanProperties().stream()
      .filter(p -> !p.hasAnnotation(Federation.class))
      .filter(p -> it._getModelMetadata().getTouched().contains(p.getName()) || (p.hasAnnotation(OneToMany.class) && it._getModelMetadata().getAction().equals(Action.DELETE)))
      .filter(p -> Model.class.isAssignableFrom(p.getType()) || (Collection.class.isAssignableFrom(p.getType()) && Model.class.isAssignableFrom(p.asArgument().getTypeParameters()[0].getType())))
      .filter(p -> isBefore == (
          (p.hasAnnotation(OneToOne.class) && p.getAnnotation(OneToOne.class).stringValue("mappedBy").isEmpty())
            || p.hasAnnotation(ManyToOne.class)
            || (p.hasAnnotation(OneToMany.class) && it._getModelMetadata().getAction().equals(Action.DELETE))
            || (p.hasAnnotation(ManyToMany.class) && p.getAnnotation(ManyToMany.class).stringValue("mappedBy").isPresent())
        )
      )
      .forEach(p -> {
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

          Optional<ModelService> service = appContext.findBean(
            new DefaultArgument<>(
              ModelService.class,
              introspection,
              Argument.of(type)
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
                  if (add.contains(o)) {
                    add.remove(o);
                  } else {
                    remove.add(o);
                  }
                });
              }
            }

            if (p.hasAnnotation(ManyToMany.class)) {
              add.stream().map(Model::getId).forEach(o -> getRepository().add(p.getName(), it.getId(), o));
              remove.stream().map(Model::getId).forEach(o -> getRepository().remove(p.getName(), it.getId(), o));
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
            values.stream()
              .filter(m -> m._getModelMetadata().isDirty())
              .forEach(subModel -> {
                UUID subModelId = subModel.getId();
                switch (subModel._getModelMetadata().getAction()) {
                  case CREATE -> service.get().create(subModel);
                  case UPDATE -> service.get().update(subModelId, subModel);
                  case DELETE ->
                    service.get().delete(subModelId);//todo should this have the reverse order of create/update?
                }
              });
          }
        }
      });
  }
}
