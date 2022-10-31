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

package org.apized.core.event;

import org.apized.core.context.ApizedContext;
import org.apized.core.event.model.Event;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public interface ESBAdapter {
  default void send(Event event) {
    Date timestamp = Optional.ofNullable(
      ApizedContext.getRequest().getTimestamp()
    ).orElse(
      Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")))
    );

    Map<String, Object> headers = new HashMap<>();
    headers.put("timestamp", timestamp);

    ApizedContext.getRequest().getPathVariables().entrySet().forEach(e ->
      headers.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : null)
    );
    headers.putAll(ApizedContext.getEvent().getHeaders());

    send(event.getId(), timestamp, event.getTopic(), headers, event.getPayload());
  }

  void send(UUID messageId, Date timestamp, String topic, Map<String, Object> headers, Map<String, Object> payload);
}
