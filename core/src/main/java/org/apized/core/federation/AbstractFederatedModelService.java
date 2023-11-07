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

package org.apized.core.federation;

import org.apized.core.error.exception.NotFoundException;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.mvc.ModelService;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractFederatedModelService<T extends Model> implements ModelService<T> {
  protected abstract ModelClient<T> getClient();

  public abstract Class<T> getType();

  @Override
  public Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort) {
    try {
      return getClient().list(page, pageSize, search, sort);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<T> searchOne(List<SearchTerm> search) {
    try {
      List<T> content = getClient().list(1, 1, search, List.of()).getContent();
      if (!content.isEmpty()) {
        return Optional.of(content.get(0));
      }
      return Optional.empty();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Page<T> list(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    try {
      return getClient().list(1, Integer.MAX_VALUE, search, sort);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T find(UUID id) {
    try {
      return Optional.ofNullable(getClient().get(id)).orElseThrow(NotFoundException::new);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T get(UUID id) {
    try {
      return getClient().get(id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T create(T it) {
    try {
      return getClient().create(it, List.of());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T update(UUID id, T it) {
    try {
      return getClient().update(it);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T delete(UUID id) {
    try {
      return getClient().delete(id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
