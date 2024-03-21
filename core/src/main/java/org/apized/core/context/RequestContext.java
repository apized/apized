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

package org.apized.core.context;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Getter
@Setter
public class RequestContext {
  private final UUID id = UUID.randomUUID();
  private final Date timestamp = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")));
  private String reason;
  private Map<String, Object> fields = new HashMap<>();
  private Map<String, UUID> pathVariables = new HashMap<>();
  private Map<String, Object> search = new HashMap<>();
  private Map<String, Object> sort = new HashMap<>();
  private Map<String, List<String>> queryParams = new HashMap<>();
}
