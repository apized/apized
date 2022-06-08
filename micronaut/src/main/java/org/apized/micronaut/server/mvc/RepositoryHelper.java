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

import org.apized.core.model.Model;
import org.apized.core.search.SearchTerm;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public abstract class RepositoryHelper {
  public static <T extends Model> QuerySpecification<T> getQuerySpecification(List<SearchTerm> search) {
    QuerySpecification<T> spec = (root, query, builder) -> {
      List<Predicate> criteria = new ArrayList<>();
      for (SearchTerm searchTerm : search) {
        final Object value = searchTerm.getValue();
        switch (searchTerm.getOp()) {
          case eq -> {
            if (value == null) {
              criteria.add(builder.isNull(root.get(searchTerm.getField())));
            } else {
              criteria.add(builder.equal(root.get(searchTerm.getField()), value));
            }
          }
          case ne -> {
            if (value == null) {
              criteria.add(builder.isNotNull(root.get(searchTerm.getField())));
            } else {
              criteria.add(builder.notEqual(root.get(searchTerm.getField()), value));
            }
          }
          case like -> criteria.add(builder.like(root.get(searchTerm.getField()), "%" + value.toString() + "%"));
          case gt -> criteria.add(builder.greaterThan(root.get(searchTerm.getField()), (Comparable) value));
          case gte -> criteria.add(builder.greaterThanOrEqualTo(root.get(searchTerm.getField()), (Comparable) value));
          case lt -> criteria.add(builder.lessThan(root.get(searchTerm.getField()), (Comparable) value));
          case lte -> criteria.add(builder.lessThanOrEqualTo(root.get(searchTerm.getField()), (Comparable) value));
        }
      }
      return builder.and(criteria.toArray(new Predicate[0]));
    };
    return spec;
  }
}
