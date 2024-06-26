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

package org.apized.micronaut.server.serde;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.naming.Named;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.DefaultArgument;
import io.micronaut.serde.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.ManyToOne;
import org.apized.core.MapHelper;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.core.context.SerdeStackEntry;
import org.apized.core.federation.Federation;
import org.apized.core.model.*;
import org.apized.core.mvc.ModelService;
import org.apized.core.security.annotation.Owner;
import org.apized.core.tracing.Traced;
import org.apized.micronaut.federation.FederationResolver;
import org.apized.micronaut.server.ModelResolver;
import org.apized.micronaut.server.mvc.ProxyRegistry;

import java.io.IOException;
import java.util.*;

@Singleton
public class ModelSerde implements Serde<Model> {
  @Inject
  ApplicationContext appContext;

  @Inject
  ProxyRegistry proxyRegistry;

  @Inject
  FederationResolver resolver;

  @Override
  @Traced(attributes = {
    @Traced.Attribute(key = "serde.entity", arg = "type")
  })
  public Model deserialize(@NonNull Decoder decoder, @NonNull DecoderContext context, Argument<? super Model> type) throws IOException {
    BeanIntrospection<? super Model> introspection = BeanIntrospection.getIntrospection(type.getType());
    boolean isId = false;
    Model model;

    try {
      String id = decoder.decodeString();
      model = appContext.findBean(new DefaultArgument<>(ModelService.class, type.getAnnotationMetadata(), type))
        .get().get(UUID.fromString(id));
      isId = true;
    } catch (IOException e) {
      model = proxyRegistry.contains(type.getType())
        ? (Model) BeanIntrospection.getIntrospection(proxyRegistry.get(type.getType())).getConstructor().instantiate((Model) null)
        : (Model) introspection.getConstructor().instantiate();
    }


    BeanWrapper<Model> deserializationWrapper = BeanWrapper.getWrapper(model);
    List<BeanProperty<? super Model, Object>> touched = new ArrayList<>();
    AnnotationValue<Apized> annotation = introspection.getAnnotation(Apized.class);
    List<Class<?>> scopes = annotation != null ? List.of(annotation.classValues("scope")) : List.of();

    if (!ApizedContext.getSerde().getStack().isEmpty()) {
      Model peekedValue = ApizedContext.getSerde().getStack().peek().getValue();
      BeanProperty<?, ?> peekedProperty = ApizedContext.getSerde().getStack().peek().getProperty();
      Class<?> peekedPropertyType = Collection.class.isAssignableFrom(peekedProperty.getType()) ? peekedProperty.asArgument().getTypeParameters()[0].getType() : peekedProperty.getType();

      introspection.getBeanProperties().stream()
        .filter(p -> scopes.size() == 0 || !scopes.stream().map(Class::getSimpleName).toList().contains(p.getName()))
        .filter(p -> !p.hasAnnotation(ApiContext.class))
        .filter(p -> p.hasAnnotation(ManyToOne.class))
        .filter(p -> peekedValue.getClass().equals(p.getType()))
        .forEach(p -> deserializationWrapper.setProperty(p.getName(), peekedValue));

      introspection.getBeanProperties().stream()
        .filter(p -> p.getAnnotation(ApiContext.class) != null)
        .filter(p -> {
          //noinspection ConstantConditions
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
          appContext.getBean(new DefaultArgument<>(ModelService.class, scopeIntrospection.getAnnotationMetadata(), Argument.of(scope)))
            .get(scopeId)
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

    String key;
    while ((key = decoder.decodeKey()) != null) {
      Optional<? extends BeanProperty<? super Model, Object>> propOpt = introspection.getProperty(key);
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
        BeanProperty<? super Model, Object> property = propOpt.get();
        touched.add(property);
        ApizedContext.getSerde().getStack().push(new SerdeStackEntry(model, property));
        if (Collection.class.isAssignableFrom(property.getType())) {
          //noinspection rawtypes
          Class subType = property.asArgument().getTypeParameters()[0].getType();
          Deserializer<?> deserializer = context.findDeserializer(subType);
          List<Object> subValues = new ArrayList<>();

          decoder.decodeArray();
          while (decoder.hasNextArrayValue()) {
            //noinspection unchecked
            subValues.add(deserializer.deserialize(decoder, context, Argument.of(subType)));
            //remove the pathVariable after the subList element deserialization so it doesn't affect other elements on the list
            ApizedContext.getRequest().getPathVariables().remove(StringHelper.uncapitalize(subType.getSimpleName()));
          }
          decoder.close();

          deserializationWrapper.setProperty(key, subValues);
        } else if (Model.class.isAssignableFrom(property.getType())) {
          Deserializer<?> deserializer = context.findDeserializer(property.getType());
          deserializationWrapper.setProperty(key, deserializer.deserialize(decoder, context, Argument.of(property.getType())));
        } else if (Map.class.isAssignableFrom(property.getType())) {
          deserializationWrapper.setProperty(key, decoder.decodeArbitrary());
        } else {
          deserializationWrapper.setProperty(key, defaultDeserialize(decoder, context, property));
        }
        ApizedContext.getSerde().getStack().pop();
      } else {
        decoder.skipValue();
      }
    }
    decoder.close();

    model.setId(model.getId() != null ? model.getId() : ApizedContext.getRequest().getPathVariables().get(StringHelper.uncapitalize(type.getTypeString(true))));
    Optional<ModelService> service = appContext.findBean(new DefaultArgument<>(ModelService.class, type.getAnnotationMetadata(), type));
    if (service.isPresent() && model.getId() != null) {
      model = service.get().get(model.getId());
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model);
      touched.forEach(p ->
        wrapper.setProperty(p.getName(), deserializationWrapper.getProperty(p.getName(), p.getType()).orElse(null))
      );
      model._getModelMetadata().setAction(Action.UPDATE);
    } else {
      model._getModelMetadata().setAction(Action.CREATE);
    }

    for (BeanProperty<? super Model, Object> property : introspection.getBeanProperties().stream().filter(p -> p.getAnnotation(Owner.class) != null).toList()) {
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model);
      boolean replace = property.getAnnotation(Owner.class).isTrue("replace");
      if (replace || wrapper.getProperty(property.getName(), UUID.class).isEmpty()) {
        wrapper.setProperty(property.getName(), ApizedContext.getSecurity().getUser().getId());
      }
    }

    model._getModelMetadata().getTouched().addAll(touched.stream().map(Named::getName).toList());

    return model;
  }

  @SuppressWarnings("unchecked")
  @Override
  @Traced(attributes = {
    @Traced.Attribute(key = "serde.entity", arg = "type")
  })
  public void serialize(Encoder encoder, @NonNull EncoderContext context, @NonNull Argument<? extends Model> type, @NonNull Model value) throws IOException {
    encoder.encodeObject(type);
    BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(value);

    String path = encoder.currentPath().replaceAll("^/", "").replaceAll("/\\d+", "");
    Map<String, Object> fields = ApizedContext.getRequest().getFields();
    Map<String, Object> search = ApizedContext.getRequest().getSearch();
    Map<String, Object> sort = ApizedContext.getRequest().getSort();
    if (!path.isBlank()) {
      for (String it : path.split("/")) {
        fields = (Map<String, Object>) fields.getOrDefault(it, new HashMap<>());
        search = (Map<String, Object>) search.getOrDefault(it, new HashMap<>());
        sort = (Map<String, Object>) sort.getOrDefault(it, new HashMap<>());
      }
    }

    Map<String, Object> finalFields = fields;
    Collection<BeanProperty<Model, Object>> properties = wrapper.getBeanProperties().stream()
      .filter(p -> !p.getAnnotationMetadata().hasAnnotation(JsonIgnore.class))
      .filter(p ->
        !p.getAnnotationMetadata().hasAnnotation(JsonProperty.class)
          || !p.getAnnotationMetadata().getAnnotation(JsonProperty.class)
          .enumValue("access", JsonProperty.Access.class)
          .get()
          .equals(JsonProperty.Access.WRITE_ONLY)
      )
      .filter(p -> p.getName().equals("id") || finalFields.isEmpty() || finalFields.containsKey("*") || finalFields.containsKey(p.getName()))
      .toList();

    for (BeanProperty<? extends Model, Object> property : properties) {
      encoder.encodeKey(property.getName());

      boolean isCollection = Collection.class.isAssignableFrom(property.getType());
      Argument<?> subType = getSubType(property, isCollection);
      boolean isModel = Model.class.isAssignableFrom(subType.getType());

      Object val;
      AnnotationValue<Federation> federation = property.getAnnotation(Federation.class);
      if (federation != null) {
        Map<String, Object> subFields = (Map<String, Object>) Optional.ofNullable(fields.get(property.getName())).orElse(Map.of());
        if (isCollection) {
          subType = Argument.of(Map.class, String.class, Object.class);
          val = ((Collection<Model>) wrapper.getProperty(property.getName(), Object.class).orElse(List.of()))
            .stream()
            .map(it ->
              resolver.resolve(
                federation.stringValue("value").orElse(null),
                federation.stringValue("type").orElse(null),
                federation.stringValue("uri").orElse(null),
                it,
                MapHelper.flatten(subFields).keySet()
              )
            )
            .toList();
        } else {
          val = resolver.resolve(
            federation.stringValue("value").orElse(null),
            federation.stringValue("type").orElse(null),
            federation.stringValue("uri").orElse(null),
            wrapper.getProperty(property.getName(), Object.class).orElse(null),
            MapHelper.flatten(subFields).keySet()
          );
        }
      } else if (!Page.class.isAssignableFrom(type.getType()) && isModel && (search.containsKey(property.getName()) || sort.containsKey(property.getName()))) {
        //noinspection RedundantCast
        val = ModelResolver.getModelValue(
          (Class<? extends BaseModel>) (Class<?>) wrapper.getIntrospection().getBeanType(),
          property.getName(),
          value.getId(),
          Collection.class.isAssignableFrom(property.getType())
            ? null
            : wrapper.getProperty(property.getName(), Model.class).orElseThrow().getId(),
          search,
          sort
        );
      } else {
        val = wrapper.getProperty(property.getName(), Object.class).orElse(null);
      }

      if (val != null) {
        if (isCollection) {
          encoder.encodeArray(property.asArgument());
          for (Object subVal : (Collection<?>) val) {
            if (isModel && (!fields.containsKey(property.getName()) || ((Map<?, ?>) fields.get(property.getName())).isEmpty())) {
              encoder.encodeString(((Model) subVal).getId().toString());
            } else {
              defaultSerialize(encoder, context, subType, subVal);
            }
          }
          encoder.finishStructure();
        } else {
          if (isModel && (!fields.containsKey(property.getName()) || ((Map<?, ?>) fields.get(property.getName())).isEmpty())) {
            encoder.encodeString(((Model) val).getId().toString());
          } else {
            defaultSerialize(encoder, context, Argument.of(val.getClass()), val);
          }
        }
      } else {
        encoder.encodeNull();
      }
    }

    encoder.finishStructure();
  }

  private Argument<?> getSubType(BeanProperty<? extends Model, Object> beanProperty, boolean isCollection) {
    Argument<?> subType;
    if (isCollection) {
      subType = beanProperty.asArgument().getTypeParameters()[0];
    } else {
      subType = beanProperty.asArgument();
    }
    return subType;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object defaultDeserialize(Decoder decoder, DecoderContext context, BeanProperty property) throws IOException {
    Deserializer deserializer = context.findDeserializer(property.asArgument());
    return deserializer.deserializeNullable(decoder, context, property.asArgument());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void defaultSerialize(Encoder encoder, EncoderContext context, Argument type, Object val) throws IOException {
    final Serializer<? super Object> serializer = context.findSerializer(type).createSpecific(context, type);
    serializer.serialize(encoder, context, type, val);
  }
}
