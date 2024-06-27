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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.naming.Named;
import org.apized.core.model.Model;

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

  @SuppressWarnings("unchecked")
  public Map<String, Object> createMapOf(Object instance) {

    Map<String, Object> result = new HashMap<>();

    if (instance != null) {
      if (instance instanceof Model) {
        BeanWrapper<Model> wrapper = BeanWrapper.getWrapper((Model) instance);
        BeanIntrospection<Model> introspection = BeanIntrospection.getIntrospection(
          instance.getClass().getSimpleName().endsWith("$Proxy")
            ? (Class<Model>) instance.getClass().getSuperclass()
            : (Class<Model>) instance.getClass()
        );

        for (BeanProperty<Model, Object> field : introspection.getBeanProperties()) {
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
                result.put(field.getName(), ((List<?>) value).stream().map(o -> retrieveValueFrom(field, o)).collect(Collectors.toList()));
              } else {
                result.put(field.getName(), retrieveValueFrom(field, value));
              }
            }
          }
        }
      } else {
        result.put("id", instance);
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getObjectMap(List<String> included, Object it) {
    Map<String, Object> result = new HashMap<>();
    BeanWrapper<Object> subWrapper = BeanWrapper.getWrapper(it);
    BeanIntrospection<Object> subIntrospection = BeanIntrospection.getIntrospection(
      it.getClass().getSimpleName().endsWith("$Proxy")
        ? (Class<Object>) it.getClass().getSuperclass()
        : (Class<Object>) it.getClass()
    );

    if (included.remove("*")) {
      included.addAll(
        subIntrospection.getBeanProperties().stream()
          .filter(p -> !(p.getAnnotation(JsonIgnore.class) != null || p.getAnnotation(ignoreAnnotation) != null))
          .map(Named::getName)
          .toList()
      );
    }

    for (String subFieldName : included) {
      if (subIntrospection.getProperty(subFieldName).isEmpty()) {
        continue;
      }

      if (subFieldName.contains(".")) {
        List<String> split = List.of(subFieldName.split("\\."));
        String directSubField = split.get(0);
        String subSubField = String.join(".", split.subList(1, split.size()));
        Object subValue = subWrapper.getProperty(directSubField, subIntrospection.getProperty(directSubField).get().getType()).orElse(null);

        if (subValue != null) {
          if (List.class.isAssignableFrom(subValue.getClass())) {
            result.put(directSubField, ((List<?>) subValue).stream().map(p -> getObjectMap(new ArrayList<>(List.of(subSubField)), p)).toList());
          } else {
            Map<String, Object> subResult = getObjectMap(new ArrayList<>(List.of(subSubField)), subValue);
            if (result.containsKey(directSubField)) {
              ((Map<String, Object>) result.get(directSubField)).putAll(subResult);
            } else {
              result.put(directSubField, subResult);
            }
          }
        }
      } else {
        Object value = subWrapper.getProperty(subFieldName, subIntrospection.getProperty(subFieldName).get().getType()).orElse(null);
        if (value != null) {
//          if (List.class.isAssignableFrom(value.getClass())) {
//            result.put(subFieldName, getObjectMap(List.of("id"), value));
//          } else
          if (value instanceof Model) {
            result.put(subFieldName, ((Model) value).getId());
          } else {
            result.put(subFieldName, value);
          }
        }
      }
    }
    return result;
  }

  private Object retrieveValueFrom(BeanProperty<Model, Object> field, Object it) {
    if (it instanceof Model) {
      if (field.getAnnotation(fieldAnnotation) != null) {
        return createMapOf(it);
      } else {
        return ((Model) it).getId();
      }
    } else {
      return it;
    }
  }
}
