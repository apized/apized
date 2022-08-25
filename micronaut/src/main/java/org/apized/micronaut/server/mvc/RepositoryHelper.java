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

import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.criteria.Predicate;
import org.apized.core.model.Model;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortDirection;
import org.apized.core.search.SortTerm;

import java.util.ArrayList;
import java.util.List;

public abstract class RepositoryHelper {
  public static <T extends Model> QuerySpecification<T> getQuerySpecification(List<SearchTerm> search) {
    QuerySpecification<T> spec = (root, query, builder) -> {
      List<Predicate> criteria = new ArrayList<>();
      for (SearchTerm searchTerm : search) {
        final String field = searchTerm.getField();
        final Object value = searchTerm.getValue();

        switch (searchTerm.getOp()) {
          case eq -> {
            if (value == null) {
              criteria.add(builder.isNull(root.get(field)));
            } else {
              criteria.add(builder.equal(root.get(field), value));
            }
          }
          case ne -> {
            if (value == null) {
              criteria.add(builder.isNotNull(root.get(field)));
            } else {
              criteria.add(builder.notEqual(root.get(field), value));
            }
          }
          case like -> criteria.add(builder.like(root.get(field), "%" + value.toString() + "%"));
          case gt -> criteria.add(builder.greaterThan(root.get(field), (Comparable) value));
          case gte -> criteria.add(builder.greaterThanOrEqualTo(root.get(field), (Comparable) value));
          case lt -> criteria.add(builder.lessThan(root.get(field), (Comparable) value));
          case lte -> criteria.add(builder.lessThanOrEqualTo(root.get(field), (Comparable) value));
        }
      }
      return builder.and(criteria.toArray(new Predicate[0]));
    };
    return spec;
  }

  public static <T extends Model> Sort generateSort(List<SortTerm> sort) {
    return Sort.of(
      sort.stream().map(s ->
          s.getDirection() == SortDirection.asc ? Sort.Order.asc(s.getField()) : Sort.Order.desc(s.getField())
        )
        .toList()
        .toArray(new Sort.Order[0]));
  }
}
