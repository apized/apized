///*
// * Copyright 2022 original authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apized.spring.audit;
//
//import io.micronaut.core.annotation.Introspected;
//import io.swagger.v3.oas.annotations.Operation;
//import jakarta.inject.Inject;
//import jakarta.transaction.Transactional;
//import org.apized.core.StringHelper;
//import org.apized.core.audit.model.AuditEntry;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Introspected
//@Transactional
//@RestController
//@RequestMapping("/audit")
//public class AuditController {
//  @Inject
//  SpringAuditEntryRepository repository;
//
//  @RequestMapping(path = "/{entity}", method = RequestMethod.GET)
//  @Operation(operationId = "Get audits for a given model", tags = { "Audit Controller" })
//  public ResponseEntity<List<AuditEntry>> getAuditForType(@PathVariable(value = "entity") String entity) {
//    List<AuditEntry> results = new ArrayList<>();
//    Iterable<AuditEntry> entries = repository.findByTypeOrderByEpochAsc(StringHelper.capitalize(StringHelper.singularize(entity)));
//    entries.iterator().forEachRemaining(results::add);
//    return ResponseEntity.status(HttpStatus.OK).body(results);
//  }
//
//  @RequestMapping(path = "/{entity}/{id}", method = RequestMethod.GET)
//  @Operation(operationId = "Get audits for a given model instance", tags = { "Audit Controller" })
//  public ResponseEntity<List<AuditEntry>> getAuditForTypeAndTarget(
//    @PathVariable(value = "entity") String entity,
//    @PathVariable(value = "id") UUID id
//  ) {
//    List<AuditEntry> results = new ArrayList<>();
//    Iterable<AuditEntry> entries = repository.findByTypeAndTargetOrderByEpochAsc(StringHelper.capitalize(StringHelper.singularize(entity)), id);
//    entries.iterator().forEachRemaining(results::add);
//    return ResponseEntity.status(HttpStatus.OK).body(results);
//  }
//}
