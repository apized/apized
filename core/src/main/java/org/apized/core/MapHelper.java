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

package org.apized.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapHelper {
  public static Map<String, Object> flatten(Map<String, Object> map) {
    return flatten(map, List.of());
  }

  public static Map<String, Object> flatten(Map<String, Object> map, List<String> path) {
    Map<String, Object> result = new HashMap();

    if (map.size() == 0 && path.size() > 0) {
      result.put(String.join(".", path) + ".", null);
    }

    map.forEach((k, v) -> {
      if (v instanceof Map) {
        List<String> subPath = new ArrayList<>(path);
        subPath.add(k);
        result.putAll(flatten((Map<String, Object>) v, subPath));
      } else {
        result.put((path.size() > 0 ? String.join(".", path) + "." : "") + k, v);
      }
    });
    return result;
  }
}
