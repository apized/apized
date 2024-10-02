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
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.inject.Inject;
//import lombok.extern.slf4j.Slf4j;
//import org.apized.core.context.ApizedContext;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//
//import java.io.IOException;
//
//@Slf4j
//@Component
//class SaveAuditsBeforeCommit {
//  @Inject
//  ObjectMapper mapper;
//
//  @Inject
//  SpringAuditEntryRepository repository;
//
//  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
//  public void beforeCommit(String event) {
//    log.debug("Saving {} audits triggered by {}.", ApizedContext.getAudit().getAuditEntries().size(), event);
//    if (!ApizedContext.getAudit().getAuditEntries().isEmpty()) {
//      repository.saveAll(ApizedContext.getAudit().getAuditEntries().values());
//      if (log.isDebugEnabled()) {
//        ApizedContext.getAudit().getAuditEntries().values().forEach((it) ->
//          {
//            try {
//              log.debug("{}[{}]: {}", it.getTarget(), it.getId(), mapper.writeValueAsString(it.getPayload()));
//            } catch (IOException e) {
//              //Do nothing
//            }
//          }
//        );
//      }
//    }
//  }
//}
