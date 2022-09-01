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
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class EventContext {
  static ThreadLocal<EventContext> threadLocalValue = ThreadLocal.withInitial(EventContext::new);

  public static EventContext getInstance() {
    return threadLocalValue.get();
  }


  private final Map<String, String> headers = new HashMap<>();
  private final Map<String, Event> events = new HashMap<>();

  public static void destroy() {
    threadLocalValue.remove();
  }

  public void add(Event entry) {
    events.put(entry.getTopic(), entry);
  }
}
