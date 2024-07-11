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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

public interface ESBAdapter {
  default void send(Event event) {
    LocalDateTime timestamp = Optional.ofNullable(
      ApizedContext.getRequest().getTimestamp()
    ).orElse(
      LocalDateTime.now(ZoneId.of("UTC"))
    );

    Map<String, Object> headers = new HashMap<>();
    headers.put("timestamp", Date.from(timestamp.toInstant(ZoneOffset.UTC)));
    headers.put("token", ApizedContext.getSecurity().getToken());

    ApizedContext.getRequest().getPathVariables().forEach((key, value) -> headers.put(key, value != null ? value.toString() : null));
    headers.putAll(ApizedContext.getEvent().getHeaders());

    send(event.getId(), timestamp, event.getTopic(), headers, event.getPayload());
  }

  void send(UUID messageId, LocalDateTime timestamp, String topic, Map<String, Object> headers, Object payload);
}
