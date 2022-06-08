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

import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelRepository<T extends Model> {
  Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort);

  Optional<T> searchOne(List<SearchTerm> search);

  Page<T> search(List<SearchTerm> search, List<SortTerm> sort);

  Optional<T> get(UUID id);

  T create(T it);

  T update(UUID id, T it);

  void delete(UUID id);
}
