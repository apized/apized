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

package org.apized.core.search;

import java.util.List;
import java.util.Optional;

public abstract class SearchHelper {
  public static SearchTerm convertTerm(String term) {
    Optional<SearchOperation> operation = resolveOperation(term);
    if (operation.isPresent()) {
      String[] parts = term.split(operation.get().getKey());
      return new SearchTerm(parts[0], operation.get(), parts.length > 1 ? parts[1] : null);
    }
    return null;
  }

  private static Optional<SearchOperation> resolveOperation(String term) {
    SearchOperation result = null;
    for (SearchOperation op : SearchOperation.values()) {
      if (term.contains(op.getKey()) && (result == null || result.getKey().length() < op.getKey().length())) {
        result = op;
      }
    }
    return Optional.ofNullable(result);
  }

  public static SortTerm convertSort(String sort) {
    if (sort.length() > 0) {
      if (List.of('<', '>').contains(sort.charAt(sort.length() - 1))) {
        return new SortTerm(
          sort.substring(0, sort.length() - 1),
          sort.charAt(sort.length() - 1) == '>' ? SortDirection.asc : SortDirection.desc
        );
      } else {
        return new SortTerm(
          sort,
          SortDirection.asc
        );
      }
    }
    return null;
  }
}
