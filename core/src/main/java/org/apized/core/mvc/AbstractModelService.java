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
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.DefaultArgument;
import jakarta.persistence.*;
import org.apized.core.error.exception.NotFoundException;
import org.apized.core.federation.Federation;
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
    return getRepository().searchOne(search);
  }

  @Override
  public Page<T> list(List<SearchTerm> search, List<SortTerm> sort) {
    return getRepository().search(search, sort);
  }

  @Override
  public T find(UUID id) {
    return getRepository().get(id).orElseThrow(NotFoundException::new);
  }

  @Override
  public T get(UUID id) {
    return getRepository().get(id).orElseThrow(NotFoundException::new);
  }

  @Override
  public T create(T it) {
    performSubExecutions(it, true);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PrePersist.class) != null)
      .findFirst()
      .ifPresent(prePersist -> prePersist.invoke(it));

    T create = getRepository().create(it);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PostPersist.class) != null)
      .findFirst()
      .ifPresent(prePersist -> prePersist.invoke(it));

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
      .findFirst()
      .ifPresent(preUpdate -> preUpdate.invoke(it));

    T update = getRepository().update(id, it);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PostUpdate.class) != null)
      .findFirst()
      .ifPresent(preUpdate -> preUpdate.invoke(it));

    performSubExecutions(it, false);
    return update;
  }

  @Override
  public T delete(UUID id) {
    T it = get(id);
    performSubExecutions(it, true);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PreRemove.class) != null)
      .findFirst()
      .ifPresent(prePersist -> prePersist.invoke(it));

    getRepository().delete(id);

    BeanIntrospection
      .getIntrospection(getType())
      .getBeanMethods()
      .stream()
      .filter(m -> m.getAnnotation(PostRemove.class) != null)
      .findFirst()
      .ifPresent(prePersist -> prePersist.invoke(it));

    performSubExecutions(it, false);
    return it;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void performSubExecutions(T it, boolean isBefore) {
    BeanWrapper<T> wrapper = BeanWrapper.getWrapper(it);
    BeanIntrospection<T> introspection = BeanIntrospection.getIntrospection(getType());

    introspection.getBeanProperties().stream()
      .filter(p -> p.getAnnotation(Federation.class) == null)
      .filter(p -> Model.class.isAssignableFrom(p.getType()) || (Collection.class.isAssignableFrom(p.getType()) && Model.class.isAssignableFrom(p.asArgument().getTypeParameters()[0].getType())))
      .filter(p -> isBefore == (
          (p.hasAnnotation(OneToOne.class) && p.getAnnotation(OneToOne.class).stringValue("mappedBy").isEmpty())
          || p.hasAnnotation(ManyToOne.class)
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
