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

package org.apized.test.integration.steps


import org.apized.test.integration.core.IntegrationConfig
import org.apized.test.integration.core.IntegrationContext
import org.apized.test.integration.core.RestIntegrationTest
import org.apized.test.integration.core.ServerConfig

abstract class AbstractBootTestServer {
  static boolean initialized = false

  abstract ServerConfig boot()

  void setup() {
    if (!initialized) {
      initialized = true
      IntegrationConfig.setTestRunner(new RestIntegrationTest(new IntegrationContext()), boot())
    }
  }
}
