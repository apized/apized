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
import java.util.UUID;

public abstract class AbstractModelController<T extends Model> implements ModelController<T> {
  protected abstract ModelService<T> getService();

  @Override
  public Page<T> list(Integer page, Integer pageSize, List<SearchTerm> search, List<SortTerm> sort) {
    if (page < 1) page = 1;
    return getService().list(page, pageSize, search, sort);
  }

  @Override
  public T get(UUID id) {
    return getService().get(id);
  }

  @Override
  public T create(T it) {
    return getService().create(it);
  }

  @Override
  public T update(UUID id, T it) {
    return getService().update(id, it);
  }

  @Override
  public T delete(UUID id) {
    return getService().delete(id);
  }
}
