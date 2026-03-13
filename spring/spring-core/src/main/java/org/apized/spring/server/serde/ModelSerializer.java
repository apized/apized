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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.type.Argument;
import org.apized.core.MapHelper;
import org.apized.core.context.ApizedContext;
import org.apized.core.federation.Federation;
import org.apized.core.model.BaseModel;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.tracing.Traced;
import org.apized.spring.federation.FederationResolver;
import org.apized.spring.server.ModelResolver;

import java.io.IOException;
import java.util.*;

public class ModelSerializer extends StdSerializer<Model> {
  private final FederationResolver resolver;

  public ModelSerializer(FederationResolver resolver) {
    this(Model.class, resolver);
  }

  protected ModelSerializer(Class<Model> t, FederationResolver resolver) {
    super(t);
    this.resolver = resolver;
  }

  @Override
  @Traced(attributes = {
    @Traced.Attribute(key = "serde.entity", arg = "type")
  })
  public void serialize(Model value, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
    gen.writeStartObject();
    BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(value);

    List<String> paths = new ArrayList<>();
    JsonStreamContext context = gen.getOutputContext();
    while (context != null) {
      paths.add(context.getCurrentName());
      context = context.getParent();
    }
    paths = paths.stream().filter(Objects::nonNull).toList().reversed();


    Map<String, Object> fields = ApizedContext.getRequest().getFields();
    Map<String, Object> search = ApizedContext.getRequest().getSearch();
    Map<String, Object> sort = ApizedContext.getRequest().getSort();
    if (!paths.isEmpty()) {
      for (String it : paths) {
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
      gen.writeFieldName(property.getName());

      boolean isCollection = Collection.class.isAssignableFrom(property.getType());
      Argument<?> subType = getSubType(property, isCollection);
      boolean isModel = Model.class.isAssignableFrom(subType.getType());

      Object val;
      AnnotationValue<Federation> federation = property.getAnnotation(Federation.class);
      if (federation != null) {
        Map<String, Object> subFields = (Map<String, Object>) Optional.ofNullable(fields.get(property.getName())).orElse(Map.of());
        if (isCollection) {
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
      } else if (!Page.class.isAssignableFrom(handledType()) && isModel && (search.containsKey(property.getName()) || sort.containsKey(property.getName()))) {
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
          gen.writeStartArray();
          for (Object subVal : (Collection<?>) val) {
            if (isModel && (!fields.containsKey(property.getName()) || ((Map<?, ?>) fields.get(property.getName())).isEmpty())) {
              gen.writeString(((Model) subVal).getId().toString());
            } else {
              gen.writeObject(subVal);
            }
          }
          gen.writeEndArray();
        } else {
          if (isModel && (!fields.containsKey(property.getName()) || ((Map<?, ?>) fields.get(property.getName())).isEmpty())) {
            gen.writeString(((Model) val).getId().toString());
          } else {
            gen.writeObject(val);
          }
        }
      } else {
        gen.writeNull();
      }
    }

    gen.writeEndObject();
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
}
