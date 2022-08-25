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

import org.apized.core.MapHelper;
import org.apized.core.StringHelper;
import org.apized.core.federation.Federated;
import org.apized.core.federation.Federation;
import org.apized.core.model.Action;
import org.apized.core.model.ApiContext;
import org.apized.core.model.Apized;
import org.apized.core.model.Model;
import org.apized.core.mvc.ModelService;
import org.apized.core.search.SearchHelper;
import org.apized.core.search.SearchOperation;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.apized.core.serde.RequestContext;
import org.apized.core.serde.SerdeContext;
import org.apized.micronaut.federation.FederationResolver;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ModelSerde implements Serde<Model> {
  @Inject
  ApplicationContext appContext;

  @Inject
  FederationResolver resolver;

  @Override
  public Model deserialize(@NonNull Decoder decoder, @NonNull DecoderContext context, Argument<? super Model> type) throws IOException {
    BeanIntrospection<? super Model> introspection = BeanIntrospection.getIntrospection(type.getType());
    Model model = (Model) introspection.getConstructor().instantiate();
    BeanWrapper<Model> deserializationWrapper = BeanWrapper.getWrapper(model);
    List<BeanProperty<? super Model, Object>> touched = new ArrayList<>();
    AnnotationValue<Apized> annotation = introspection.getAnnotation(Apized.class);
    Class<?> scope = annotation != null ? annotation.classValue("scope").orElse(null) : null;

    if (SerdeContext.getInstance().size() > 0) {
      Model peekedValue = SerdeContext.getInstance().peek().getValue();
      BeanProperty<?, ?> peekedProperty = SerdeContext.getInstance().peek().getProperty();
      Class<?> peekedPropertyType = Collection.class.isAssignableFrom(peekedProperty.getType()) ? peekedProperty.asArgument().getTypeParameters()[0].getType() : peekedProperty.getType();

      introspection.getBeanProperties().stream()
        .filter(p -> scope == null || !p.getName().equals(StringHelper.uncapitalize(scope.getSimpleName())))
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

    if (scope != null) {
      BeanIntrospection<?> scopeIntrospection = BeanIntrospection.getIntrospection(scope);
      String scopeTypeName = StringHelper.uncapitalize(scope.getSimpleName());
      UUID scopeId = RequestContext.getInstance().getPathVariables().get(scopeTypeName);
      if (scopeId != null) {
        deserializationWrapper.setProperty(
          scopeTypeName,
          appContext.getBean(new DefaultArgument<>(ModelService.class, scopeIntrospection.getAnnotationMetadata(), Argument.of(scope)))
            .get(scopeId)
        );
      } else if (SerdeContext.getInstance().stream().anyMatch(e -> StringHelper.uncapitalize(e.getValue().getClass().getSimpleName()).equals(scopeTypeName))) {
        deserializationWrapper.setProperty(
          scopeTypeName,
          SerdeContext.getInstance().stream().map(SerdeContext.SerdeStackEntry::getValue).filter(e -> StringHelper.uncapitalize(e.getClass().getSimpleName()).equals(scopeTypeName)).findFirst().orElse(null)
        );
      }
    }

    decoder.decodeObject();
    String key;
    while ((key = decoder.decodeKey()) != null) {
      Optional<? extends BeanProperty<? super Model, Object>> propOpt = introspection.getProperty(key);
      if (propOpt.isPresent()) {
        BeanProperty<? super Model, Object> property = propOpt.get();
        touched.add(property);
        SerdeContext.getInstance().push(new SerdeContext.SerdeStackEntry(model, property));
        if (Collection.class.isAssignableFrom(property.getType()) && Model.class.isAssignableFrom(property.asArgument().getTypeParameters()[0].getType())) {
          //noinspection rawtypes
          Class subType = property.asArgument().getTypeParameters()[0].getType();
          Deserializer<?> deserializer = context.findDeserializer(subType);
          List<Model> subValues = new ArrayList<>();

          decoder.decodeArray();
          while (decoder.hasNextArrayValue()) {
            //noinspection unchecked
            subValues.add((Model) deserializer.deserialize(decoder, context, Argument.of(subType)));
          }
          decoder.close();

          deserializationWrapper.setProperty(key, subValues);
        } else if (Model.class.isAssignableFrom(property.getType())) {
          Deserializer<?> deserializer = context.findDeserializer(property.getType());
          deserializationWrapper.setProperty(key, deserializer.deserialize(decoder, context, Argument.of(property.getType())));
        } else {
          deserializationWrapper.setProperty(key, decoder.decodeArbitrary());
        }
        SerdeContext.getInstance().pop();
      }
    }
    decoder.close();

    if (model.getId() != null) {
      ModelService<?> service = appContext.getBean(new DefaultArgument<>(ModelService.class, type.getAnnotationMetadata(), type));
      model = (Model) service.get(model.getId());
      BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model);
      touched.forEach(p -> wrapper.setProperty(p.getName(), deserializationWrapper.getProperty(p.getName(), p.getType())));
      model._getModelMetadata().setAction(Action.UPDATE);
    } else {
      model._getModelMetadata().setAction(Action.CREATE);
    }
    model._getModelMetadata().getTouched().addAll(touched.stream().map(Named::getName).toList());

    if (Federated.class.isAssignableFrom(model.getClass()) && model.getId() == null) {
      model.setId(UUID.randomUUID());
    }

    return model;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void serialize(Encoder encoder, @NonNull EncoderContext context, @NonNull Argument<? extends Model> type, @NonNull Model value) throws IOException {
    encoder.encodeObject(type);
    BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(value);

    String path = encoder.currentPath().replaceAll("^/", "").replaceAll("/\\d+", "");
    Map<String, Object> fields = RequestContext.getInstance().getFields();
    Map<String, Object> search = RequestContext.getInstance().getSearch();
    Map<String, Object> sort = RequestContext.getInstance().getSort();
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
      .filter(p -> p.getName().equals("id") || finalFields.isEmpty() || finalFields.containsKey("*") || finalFields.containsKey(p.getName()))
      .toList();

    for (BeanProperty<? extends Model, Object> property : properties) {
      encoder.encodeKey(property.getName());

      boolean isCollection = Collection.class.isAssignableFrom(property.getType());
      Argument<?> subType = getSubType(property, isCollection);
      boolean isModel = Model.class.isAssignableFrom(subType.getType());

      Object val;
      if (isModel) {
        Map<String, Object> subFields = (Map<String, Object>) Optional.ofNullable(fields.get(property.getName())).orElse(Map.of());
        val = getModelValue(wrapper, property, subType, isCollection, search, sort, MapHelper.flatten(subFields).keySet());
      } else {
        val = wrapper.getProperty(property.getName(), Object.class).orElse(null);
      }

      if (val != null) {
        if (isCollection) {
          encoder.encodeArray(property.asArgument());
          for (Object subVal : (Collection<?>) val) {
            if (isModel && !fields.containsKey(property.getName())) {
              encoder.encodeString(((Model) subVal).getId().toString());
            } else {
              defaultSerialize(encoder, context, subType, subVal);
            }
          }
          encoder.finishStructure();
        } else {
          if (isModel && !fields.containsKey(property.getName())) {
            UUID id = ((Model) val).getId();
            encoder.encodeObject(property.asArgument());
            encoder.encodeKey("id");
            if (id != null) {
              encoder.encodeString(id.toString());
            } else {
              encoder.encodeNull();
            }
            encoder.finishStructure();
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

  @SuppressWarnings("unchecked")
  private Object getModelValue(BeanWrapper<Model> wrapper, BeanProperty<? extends Model, Object> property, Argument<?> subType, boolean isCollection, Map<String, Object> search, Map<String, Object> sort, Set<String> fields) {
    List<SearchTerm> terms = ((Map<String, Object>) search.getOrDefault(property.getName(), new HashMap<String, Object>())).keySet().stream().map(SearchHelper::convertTerm).filter(Objects::nonNull).collect(Collectors.toList());
    List<SortTerm> subSort = ((Map<String, Object>) sort.getOrDefault(property.getName(), new HashMap<String, Object>())).keySet().stream().map(SearchHelper::convertSort).filter(Objects::nonNull).collect(Collectors.toList());
    AnnotationValue<Federation> federation = property.getAnnotation(Federation.class);
    ModelService<?> service = federation == null ? appContext.getBean(new DefaultArgument<>(ModelService.class, subType.getAnnotationMetadata(), subType)) : null;

    if (isCollection) {
      //todo get limits from the request properties
      Optional<AnnotationValue<OneToMany>> oneToMany = property.getAnnotationMetadata().findAnnotation(OneToMany.class);
      if (oneToMany.isPresent()) {
        Optional<String> mappedBy = oneToMany.flatMap(annotation -> annotation.stringValue("mappedBy"));
        terms.add(new SearchTerm(mappedBy.isEmpty() ? property.getName() : mappedBy.get(), SearchOperation.eq, wrapper.getProperty("id", UUID.class).orElse(null)));
      } else {
        terms.add(new SearchTerm(StringHelper.uncapitalize(StringHelper.pluralize(property.getDeclaringType().getSimpleName())), SearchOperation.eq, List.of(wrapper.getProperty("id", UUID.class).orElse(UUID.randomUUID()))));
      }
      return service.list(terms, subSort).getContent();
    } else {
      if (federation != null) {
        return resolver.resolve(
          federation.stringValue("value").orElse(null),
          federation.stringValue("type").orElse(null),
          federation.stringValue("uri").orElse(null),
          wrapper.getProperty(property.getName(), Federated.class).orElse(null),
          fields
        );
      } else {
        Optional<String> mappedBy = property.getAnnotationMetadata().findAnnotation(OneToOne.class).flatMap(annotation -> annotation.stringValue("mappedBy"));
        if (mappedBy.isEmpty()) {
          Model model = wrapper.getProperty(property.getName(), Model.class).orElse(null);
          return model == null ? null : service.get(model.getId());
        } else {
          terms.add(new SearchTerm(mappedBy.get(), SearchOperation.eq, wrapper.getProperty("id", UUID.class).orElse(null)));
          return service.list(terms, subSort).getContent().stream().findFirst().orElse(null);
        }
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void defaultSerialize(Encoder encoder, EncoderContext context, Argument type, Object val) throws IOException {
    final Serializer<? super Object> serializer = context.findSerializer(type).createSpecific(context, type);
    serializer.serialize(encoder, context, type, val);
  }
}
