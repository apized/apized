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

package org.apized.core.security.enricher;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.error.exception.ServerException;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Singleton
public class PermissionEnricherManager {
  List<PermissionEnricherEntry> registry = new ArrayList<>();

  public void registerEnricher(Class<? extends Model> type, PermissionEnricherHandler<? extends Model> enricher) {
    registry.add(new PermissionEnricherEntry(type, enricher));
  }

  public boolean executeEnrichersFor(Class<Model> type, Action action, Execution execution) {
    try {
      List<PermissionEnricherEntry> enricherEntries = registry.stream()
        .filter(e -> e.type().isAssignableFrom(type))
        .toList();

      if (!enricherEntries.isEmpty()) {
        return enricherEntries.stream().map(it -> it.enricher().enrich(type, action, execution)).reduce(false, Boolean::logicalOr);
      }
      return false;
    } catch (Exception e) {
      if (!(e instanceof ServerException)) {
        log.error("Error occurred running enricher for '" + type + "'", e);
      }
      throw e;
    }
  }
}
