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

package org.apized.spring.server.mvc;

import org.apized.core.model.BaseModel;
import org.apized.core.model.Page;
import org.apized.core.mvc.ApizedRepository;
import org.apized.core.mvc.ManyToManyTuple;
import org.apized.core.mvc.ModelRepository;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApizedRepository
public interface SpringModelRepository<T extends BaseModel> extends ModelRepository<T>, JpaRepositoryImplementation<T, UUID> {

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
    org.springframework.data.domain.Page<T> result = findAll(
      (Specification<T>) (root, query, criteriaBuilder) -> CriteriaHelper.applySearch(root, query, criteriaBuilder, search, skipAutoFilters),
      PageRequest.of(page - 1, pageSize, CriteriaHelper.generateSort(sort))
    );

    return Page.<T>builder()
      .page(page)
      .pageSize(pageSize)
      .totalPages(result.getTotalPages())
      .total(result.getTotalElements())
      .content(result.getContent())
      .build();
  }

  @Override
  default Optional<T> searchOne(List<SearchTerm> search) {
    return findOne((Specification<T>) (root, query, criteriaBuilder) -> CriteriaHelper.applySearch(root, query, criteriaBuilder, search));
  }

  @Override
  default Page<T> search(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    var results = findAll(
      (Specification<T>) (root, query, criteriaBuilder) -> CriteriaHelper.applySearch(root, query, criteriaBuilder, search, skipAutoFilters),
      CriteriaHelper.generateSort(sort)
    );

    return Page.<T>builder().content(results).build();
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
    return save(it);
  }

  @Override
  default List<T> batchUpdate(List<T> it) {
    return saveAll(it);
  }

  @Override
  default void delete(UUID it) {
    deleteById(it);
  }

  @Override
  default void batchDelete(List<UUID> it) {
    deleteAllById(it);
  }
}
