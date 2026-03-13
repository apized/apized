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

package org.apized.spring.server.serde;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.naming.Named;
import jakarta.inject.Inject;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.core.context.SerdeStackEntry;
import org.apized.core.model.Action;
import org.apized.core.model.ApiContext;
import org.apized.core.model.Apized;
import org.apized.core.model.Model;
import org.apized.core.mvc.ModelService;
import org.apized.core.security.annotation.Owner;
import org.apized.core.tracing.Traced;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.util.*;

public class ModelDeserializer<T extends Model> extends JsonDeserializer<T> {
  @Inject
  ApplicationContext appContext;

  @Getter
  private final Class<T> type;

  public ModelDeserializer(Class<T> type) {
    this.type = type;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  @Traced(attributes = {
    @Traced.Attribute(key = "serde.entity", arg = "type")
  })
  public T deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException, JacksonException {
    var introspection = BeanIntrospection.getIntrospection(type);
    boolean isId = false;
    T model = null;

    if (parser.currentToken().equals(JsonToken.VALUE_STRING)) {
      String id = parser.getText();
      model = (T) getModelServiceFor(type).get().get(UUID.fromString(id));
      isId = true;
    } else {
      model = introspection.getConstructor().instantiate();
    }

    BeanWrapper<Model> deserializationWrapper = BeanWrapper.getWrapper(model);
    List<BeanProperty<T, Object>> touched = new ArrayList<>();
    AnnotationValue<Apized> annotation = introspection.getAnnotation(Apized.class);
    List<Class<?>> scopes = annotation != null ? List.of(annotation.classValues("scope")) : List.of();

    if (!ApizedContext.getSerde().getStack().isEmpty()) {
      Model peekedValue = ApizedContext.getSerde().getStack().peek().getValue();
      BeanProperty<?, ?> peekedProperty = ApizedContext.getSerde().getStack().peek().getProperty();
      Class<?> peekedPropertyType = Collection.class.isAssignableFrom(peekedProperty.getType()) ? peekedProperty.asArgument().getTypeParameters()[0].getType() : peekedProperty.getType();

      introspection.getBeanProperties().stream()
        .filter(p -> scopes.isEmpty() || !scopes.stream().map(Class::getSimpleName).toList().contains(p.getName()))
        .filter(p -> !p.hasAnnotation(ApiContext.class))
        .filter(p -> p.hasAnnotation(ManyToOne.class))
        .filter(p -> peekedValue.getClass().equals(p.getType()))
        .forEach(p -> deserializationWrapper.setProperty(p.getName(), peekedValue));

      introspection.getBeanProperties().stream()
        .filter(p -> p.getAnnotation(ApiContext.class) != null)
        .filter(p -> {
          String property = p.getAnnotation(ApiContext.class).getRequiredValue("property", String.class);
          return (peekedPropertyType.equals(p.getType())
            || peekedValue.getClass().equals(p.getType())) && (property.isEmpty() || peekedProperty.getName().equals(property));
        })
        .forEach(p -> deserializationWrapper.setProperty(p.getName(), peekedValue));
    }

    for (Class<?> scope : scopes) {
      BeanIntrospection<?> scopeIntrospection = BeanIntrospection.getIntrospection(scope);
      String scopeTypeName = StringHelper.uncapitalize(scope.getSimpleName());
      UUID scopeId = ApizedContext.getRequest().getPathVariables().get(scopeTypeName);
      if (scopeId != null) {
        deserializationWrapper.setProperty(
          scopeTypeName,
          ((ModelService<Model>) appContext.getBean(appContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(ModelService.class, scope))[0])).get(scopeId)
        );
      } else if (ApizedContext.getSerde().getStack().stream().anyMatch(e -> StringHelper.uncapitalize(e.getValue().getClass().getSimpleName().replaceAll("\\$Proxy", "")).equals(scopeTypeName))) {
        deserializationWrapper.setProperty(
          scopeTypeName,
          ApizedContext.getSerde().getStack().stream().map(SerdeStackEntry::getValue).filter(e -> StringHelper.uncapitalize(e.getClass().getSimpleName().replaceAll("\\$Proxy", "")).equals(scopeTypeName)).findFirst().orElse(null)
        );
      }
    }

    if (isId) {
      return model;
    }

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String key = parser.getText();
      parser.nextToken();
      var propOpt = introspection.getProperty(key);
      if (
        propOpt.isPresent()
          && propOpt.get().getAnnotation(JsonIgnore.class) == null
          && (
          key.equals("id") ||
            propOpt.get().getAnnotation(JsonProperty.class) == null
            || !propOpt.get().getAnnotation(JsonProperty.class)
            .enumValue("access", JsonProperty.Access.class)
            .get()
            .equals(JsonProperty.Access.READ_ONLY)
        )
      ) {
        var property = propOpt.get();
        touched.add(property);
        ApizedContext.getSerde().getStack().push(new SerdeStackEntry(model, property));
        if (Collection.class.isAssignableFrom(property.getType())) {
          Class subType = property.asArgument().getTypeParameters()[0].getType();
          List<Object> subValues = new ArrayList<>();

          while (parser.nextToken() != JsonToken.END_ARRAY) {
            subValues.add(parser.readValueAs(subType));
            ApizedContext.getRequest().getPathVariables().remove(StringHelper.uncapitalize(subType.getSimpleName()));
          }

          deserializationWrapper.setProperty(key, subValues);
        } else if (Model.class.isAssignableFrom(property.getType())) {
          deserializationWrapper.setProperty(key, parser.readValueAs(property.getType()));
        } else if (Map.class.isAssignableFrom(property.getType())) {
          deserializationWrapper.setProperty(key, parser.readValueAs(Map.class));
        } else {
          deserializationWrapper.setProperty(key, parser.getValueAsString());
        }
        ApizedContext.getSerde().getStack().pop();
      } else {
        System.out.println();
//        parser.skipValue();
      }
    }

    model.setId(model.getId() != null ? model.getId() : ApizedContext.getRequest().getPathVariables().get(StringHelper.uncapitalize(type.getSimpleName())));
    var service = getModelServiceFor(type);
    if (service.isPresent() && model.getId() != null) {
      model = (T) service.get().get(model.getId());
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model);
      touched.forEach(p ->
        wrapper.setProperty(p.getName(), deserializationWrapper.getProperty(p.getName(), p.getType()).orElse(null))
      );
      model._getModelMetadata().setAction(Action.UPDATE);
    } else {
      model._getModelMetadata().setAction(Action.CREATE);
    }

    for (BeanProperty<T, Object> property : introspection.getBeanProperties().stream().filter(p -> p.getAnnotation(Owner.class) != null).toList()) {
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model);
      boolean replace = property.getAnnotation(Owner.class).isTrue("replace");
      if (replace || wrapper.getProperty(property.getName(), UUID.class).isEmpty()) {
        wrapper.setProperty(property.getName(), ApizedContext.getSecurity().getUser().getId());
      }
    }

    model._getModelMetadata().getTouched().addAll(touched.stream().map(Named::getName).toList());

    return model;
  }

  private Optional<ModelService<? extends Model>> getModelServiceFor(Class<? extends Model> type) {
    try {
      List<String> names = List.of(appContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(ModelService.class, type)));
      return Optional.ofNullable(names.isEmpty() ? null : (ModelService<? extends Model>) appContext.getBean(names.getFirst()));
    } catch (NoSuchBeanDefinitionException e) {
      return Optional.empty();
    }
  }
}
