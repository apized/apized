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

package org.apized.core.model;

import java.util.Optional;

public enum Action {
  LIST("list", "LIST"),
  GET("get", "GET"),
  CREATE("create", "POST"),
  UPDATE("update", "PUT"),
  DELETE("delete", "DELETE"),
  NO_OP(null, null);

  private final String type;
  private final String verb;

  Action(String type, String verb) {
    this.type = type;
    this.verb = verb;
  }

  public String getVerb() {
    return verb;
  }

  public String getType() {
    return type;
  }

  public static Action fromVerb(String verb) {
    for (Action b : Action.values()) {
      if (Optional.ofNullable(b.getVerb()).orElse("").equalsIgnoreCase(verb)) {
        return b;
      }
    }
    return NO_OP;
  }
}

