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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor;
import io.micronaut.data.repository.jpa.criteria.DeleteSpecification;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.mvc.ManyToManyTuple;
import org.apized.core.mvc.ModelRepository;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApizedRepository
public interface MicronautModelRepository<T extends Model> extends ModelRepository<T>, PageableRepository<T, UUID>, JpaSpecificationExecutor<T> {
  @Override
  default void add(String field, UUID self, UUID other) {
  }

  @Override
  default void addMany(String field, List<ManyToManyTuple> adds) {
  }

  @Override
  default void remove(String field, UUID self, UUID other) {
  }

  @Override
  default void removeMany(String field, List<ManyToManyTuple> removes) {
  }

  @Override
  default Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    QuerySpecification<T> spec = RepositoryHelper.getQuerySpecification(new ArrayList<>(search), skipAutoFilters);

    io.micronaut.data.model.Page<T> micronautPage = findAll(
      spec,
      Pageable.from(page - 1, pageSize, RepositoryHelper.generateSort(sort))
    );

    return Page.<T>builder()
      .page(page)
      .pageSize(pageSize)
      .totalPages(micronautPage.getTotalPages())
      .total(micronautPage.getTotalSize())
      .content(micronautPage.getContent())
      .build();
  }

  @Override
  default Optional<T> searchOne(List<SearchTerm> search) {
    QuerySpecification<T> spec = RepositoryHelper.getQuerySpecification(new ArrayList<>(search));
    return findOne(spec);
  }

  @Override
  default Page<T> search(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    QuerySpecification<T> spec = RepositoryHelper.getQuerySpecification(new ArrayList<>(search), skipAutoFilters);
    return Page.<T>builder().content(
      new ArrayList<>(findAll(spec, RepositoryHelper.generateSort(sort)))
    ).build();
  }

  default Optional<T> get(UUID id) {
    return findById(id);
  }

  @Override
  default T create(T it) {
    return save(it);
  }

  @Override
  default List<T> batchCreate(List<T> it) {
    return saveAll(it);
  }

  @Override
  default T update(UUID id, T it) {
    return update(it);
  }

  @Override
  default List<T> batchUpdate(List<T> it) {
    return updateAll(it);
  }

  @Override
  default void delete(UUID it) {
    deleteById(it);
  }

  @Override
  default void batchDelete(List<UUID> it) {
    deleteAll(new DeleteSpecification<T>() {
      @Override
      public @Nullable Predicate toPredicate(@NonNull Root<T> root, @NonNull CriteriaDelete<?> query, @NonNull CriteriaBuilder criteriaBuilder) {
        return root.get("id").in(it);
      }
    });
  }
}
