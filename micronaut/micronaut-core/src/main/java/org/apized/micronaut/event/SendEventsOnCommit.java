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

package org.apized.micronaut.event;

import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.transaction.annotation.TransactionalEventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.context.ApizedContext;
import org.apized.core.event.ESBAdapter;

import java.io.IOException;

@Slf4j
@Singleton
@Requires(bean = ESBAdapter.class)
class SendEventsOnCommit {
  @Inject
  ObjectMapper mapper;

  @Inject
  ESBAdapter esb;

  @TransactionalEventListener(TransactionalEventListener.TransactionPhase.AFTER_COMMIT)
  public void afterCommit(String event) {
    log.debug("Sending {} events triggered by {}.", ApizedContext.getEvent().getEvents().size(), event);
    ApizedContext.getEvent().getEvents().values().forEach(esb::send);
    ApizedContext.getEvent().getEvents().values().forEach(it -> {
      try {
        log.debug("{}: {}", it.getTopic(), mapper.writeValueAsString(it.getPayload()));
      } catch (IOException e) {
        //Do nothing
      }
    });
  }
}
