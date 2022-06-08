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

package org.apized.test.integration.core


import org.apized.core.model.Model

class IntegrationConfig {
  static ThreadLocal<TestRunner> threadLocal = new ThreadLocal<>()
  static IntegrationConfig instance

  int port
  List<Class<Model>> types

  static void setTestRunner(TestRunner testRunner, ServerConfig config) {
    threadLocal.set(testRunner)
    if (!instance) {
      instance = new IntegrationConfig()
      instance.port = config.port
      instance.types = config.types
    }
  }

  static TestRunner getTestRunner() {
    threadLocal.get()
  }

  static IntegrationConfig getInstance() {
    instance
  }

  private IntegrationConfig() {}
}

