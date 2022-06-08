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

import org.apized.core.error.exception.NotImplementedException;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelService<T extends Model> {
  default Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort) {
    throw new NotImplementedException();
  }

  default List<T> searchAll(SearchTerm... search) {
    return searchAll(Arrays.asList(search));
  }

  default List<T> searchAll(List<SearchTerm> search) {
    return list(search, List.of()).getContent();
  }

  default Page<T> list(List<SearchTerm> search, List<SortTerm> sort) {
    throw new NotImplementedException();
  }

  default Optional<T> searchOne(SearchTerm... search) {
    return searchOne(Arrays.asList(search));
  }

  default Optional<T> searchOne(List<SearchTerm> search) {
    throw new NotImplementedException();
  }

  default T get(UUID id) {
    throw new NotImplementedException();
  }

  default T create(T it) {
    throw new NotImplementedException();
  }

  default T update(UUID id, T it) {
    throw new NotImplementedException();
  }

  default T upsert(T it) {
    if (it.getId() == null) {
      return create(it);
    } else {
      return update(it.getId(), it);
    }
  }

  default T delete(UUID id) {
    throw new NotImplementedException();
  }
}
