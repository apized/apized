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

package org.apized.core;

import org.apized.core.audit.annotation.AuditField;
import org.apized.core.model.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class ModelMapper {

  private final Class<? extends Annotation> fieldAnnotation;

  private final Class<? extends Annotation> ignoreAnnotation;

  public ModelMapper(Class<? extends Annotation> fieldAnnotation, Class<? extends Annotation> ignoreAnnotation) {
    this.fieldAnnotation = fieldAnnotation;
    this.ignoreAnnotation = ignoreAnnotation;
  }

  public Map<String, Object> createMapOf(Model instance) {
    Map<String, Object> result = new HashMap<>();

    if (instance != null) {
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(instance);

      for (BeanProperty<Model, Object> field : wrapper.getBeanProperties()) {
        boolean ignore = field.getAnnotation(fieldAnnotation) == null && (field.getAnnotation(JsonIgnore.class) != null || field.getAnnotation(ignoreAnnotation) != null);

        if (!ignore) {
          List<String> included = new ArrayList<>();
          if (field.getAnnotation(fieldAnnotation) != null) {
            Optional.ofNullable(field.getAnnotation(fieldAnnotation)).ifPresent((annotation -> included.addAll(Arrays.asList(annotation.stringValues("value")))));
          }

          Object value = wrapper.getProperty(field.getName(), field.getType()).orElse(null);
          if (included.size() > 0 && value != null) {
            if (List.class.isAssignableFrom(value.getClass())) {
              result.put(field.getName(), ((List<?>) value).stream().map(it -> getObjectMap(included, it)).collect(Collectors.toList()));
            } else {
              result.put(field.getName(), getObjectMap(included, value));
            }
          } else if (value != null) {
            if (List.class.isAssignableFrom(value.getClass())) {
              result.put(field.getName(), ((List<?>) value).stream().map(this::retrieveValueFrom).collect(Collectors.toList()));
            } else {
              result.put(field.getName(), retrieveValueFrom(value));
            }
          }
        }
      }
    }

    return result;
  }

  private Map<String, Object> getObjectMap(List<String> included, Object it) {
    Map<String, Object> subResult = new HashMap<>();
    for (String subFieldName : included) {
      BeanWrapper<Object> subWrapper = BeanWrapper.getWrapper(it);
      BeanIntrospection<Object> subIntrospection = subWrapper.getIntrospection();
      subResult.put(subFieldName, subWrapper.getProperty(subFieldName, subIntrospection.getProperty(subFieldName).get().getType()));
    }
    return subResult;
  }

  private Object retrieveValueFrom(Object it) {
    if (it instanceof Model) {
      if (BeanIntrospection.getIntrospection(it.getClass()).getAnnotation(AuditField.class) != null) {
        return createMapOf((Model) it);
      } else {
        return ((Model) it).getId();
      }
    } else {
      return it;
    }
  }
}
