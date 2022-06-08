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

package org.apized.micronaut.server.mvc;

import org.apized.core.search.SearchHelper;
import org.apized.core.search.SearchTerm;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
class SearchTermConverter implements TypeConverter<String, SearchTerm> {

  @Override
  public Optional<SearchTerm> convert(String term, Class<SearchTerm> targetType, ConversionContext context) {
    SearchTerm convert = SearchHelper.convert(term);
    if (convert != null && convert.getField().contains(".")) {
      convert = null;
    }
    return Optional.ofNullable(convert);
  }
}
