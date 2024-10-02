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

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Properties to configure apized.
 */
@Data
public class ApizedConfig {

  @Setter
  @Getter
  private static ApizedConfig instance;

  @Getter
  protected static String engine;

  /**
   * The SQL dialect for this application.
   */
  private Dialect dialect;

  /**
   * The slug for this application. This will be used as the base for permissions.
   */
  private String slug;

  /**
   * The cookie name to look for a security token.
   */
  private String cookie = "token";

  /**
   * The token for this application to use. Only needed if security is enabled (by providing an implementation of UserResolver)
   */
  private String token;

  /**
   * The urls to exclude from the Apized filters
   */
  List<String> exclusions;

  /**
   * Known apis that can be used for federation.
   */
  private Map<String, String> federation = new HashMap<>();
}
