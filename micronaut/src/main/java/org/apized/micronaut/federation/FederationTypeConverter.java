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

package org.apized.micronaut.federation;

import org.apized.core.federation.Federated;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class FederationTypeConverter implements TypeConverter<String, Federated> {
  @Inject
  ObjectMapper mapper;

  @Override
  public Optional<Federated> convert(String object, Class<Federated> targetType) {
    return TypeConverter.super.convert(object, targetType);
  }

  @Override
  public Optional<Federated> convert(String object, Class<Federated> targetType, ConversionContext context) {
    Federated result = null;
    try {
      Federated instantiate = BeanIntrospection.getIntrospection(targetType).instantiate();
      BeanWrapper<Federated> wrapper = BeanWrapper.getWrapper(instantiate);
      mapper.readValue(object, Map.class).forEach((k, v) -> {
        wrapper.setProperty(k.toString(), v);
      });
      result=instantiate;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return Optional.ofNullable(result);
  }
}
