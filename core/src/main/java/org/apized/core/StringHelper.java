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

import java.util.UUID;

public abstract class StringHelper {
  public static String uncapitalize(String string) {
    if (string == null || string.length() == 0) {
      return string;
    }

    char c[] = string.toCharArray();
    c[0] = Character.toLowerCase(c[0]);

    return new String(c);
  }

  public static String capitalize(String string) {
    if (string == null || string.length() == 0) {
      return string;
    }

    char c[] = string.toCharArray();
    c[0] = Character.toUpperCase(c[0]);

    return new String(c);
  }

  public static String pluralize(String name) {
    if (name.endsWith("ry")) {
      return name.substring(0, name.length() - 1) + "ies";
    } else if (name.endsWith("x")) {
      return name.substring(0, name.length() - 1) + "xes";
    } else {
      return name + "s";
    }
  }

  public static String singularize(String name) {
    if (name.endsWith("ies")) {
      return name.substring(0, name.length() - 3) + "y";
    } else if (name.endsWith("xes")) {
      return name.substring(0, name.length() - 2);
    } else {
      return name.substring(0, name.length() - 1);
    }
  }

  public static UUID convertStringToUUID(String uuid) {
    return StringHelper.convertStringToUUID(uuid, null);
  }

  public static UUID convertStringToUUID(String uuid, UUID fallback) {
    try {
      return uuid != null && !uuid.isEmpty() ? UUID.fromString(uuid) : fallback;
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }
}
