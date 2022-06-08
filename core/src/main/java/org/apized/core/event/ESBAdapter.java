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

import org.apized.core.event.model.Event;
import org.apized.core.serde.RequestContext;

import java.util.HashMap;
import java.util.Map;

public interface ESBAdapter {
  default void send(Event event) {
    sendInEnvelope(event.getTopic(), event.getPayload());
  }

  default void sendInEnvelope(String topic, Map<String, Object> payload) {
    Map<String, String> headers = new HashMap<>();
    RequestContext.getInstance().getPathVariables().entrySet().forEach(e ->
      headers.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : null)
    );
    headers.putAll(EventContext.getInstance().getHeaders());

    send(topic, Map.of("header", headers, "payload", payload));
  }

  void send(String topic, Map<String, Object> payload);
}
