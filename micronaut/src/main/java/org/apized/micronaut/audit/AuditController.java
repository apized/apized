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

package org.apized.micronaut.audit;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;
import org.apized.core.StringHelper;
import org.apized.core.audit.model.AuditEntry;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Introspected
@Transactional
@Controller("/audit/")
public class AuditController {
  @Inject
  AuditEntryRepository repository;

  @Get("/{entity}")
  @Operation(operationId = "Get audits for a given model", tags = {"Audit Controller"})
  public HttpResponse<List<AuditEntry>> getAuditForType(@PathVariable(value = "entity") String entity) {
    List<AuditEntry> results = new ArrayList<>();
    Iterable<AuditEntry> entries = repository.findByTypeOrderByTimestampAsc(StringHelper.capitalize(StringHelper.singularize(entity)));
    entries.iterator().forEachRemaining(results::add);
    return HttpResponse.status(HttpStatus.OK).body(results);
  }

  @Get("/{entity}/{id}")
  @Operation(operationId = "Get audits for a given model instance", tags = {"Audit Controller"})
  public HttpResponse<List<AuditEntry>> getAuditForTypeAndTarget(
    @PathVariable(value = "entity") String entity,
    @PathVariable(value = "id") UUID id
  ) {
    List<AuditEntry> results = new ArrayList<>();
    Iterable<AuditEntry> entries = repository.findByTypeAndTargetOrderByTimestampAsc(StringHelper.capitalize(StringHelper.singularize(entity)), id);
    entries.iterator().forEachRemaining(results::add);
    return HttpResponse.status(HttpStatus.OK).body(results);
  }
}
