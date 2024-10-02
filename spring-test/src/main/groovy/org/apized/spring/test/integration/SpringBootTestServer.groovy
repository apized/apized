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

package org.apized.spring.test.integration

import io.cucumber.java.BeforeAll
import org.apized.spring.server.mvc.SpringModelService
import org.apized.test.integration.core.IntegrationConfig
import org.apized.test.integration.core.IntegrationContext
import org.apized.test.integration.core.RestIntegrationTest
import org.apized.test.integration.core.ServerConfig
import org.apized.test.integration.mocks.AbstractUserResolverMock
import org.springframework.boot.SpringApplication
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext

@SuppressWarnings('unused')
class SpringBootTestServer {
  static boolean initialized = false
  static private SpringApplication application
  static private ServletWebServerApplicationContext webApplicationContext

  @BeforeAll(order = 1)
  static void setup() {
    if (!initialized) {
      initialized = true
      IntegrationConfig.setTestRunner(new RestIntegrationTest(new IntegrationContext()), boot())
      IntegrationConfig.testRunner.context.responses.put(
        "users",
        webApplicationContext.getBean(AbstractUserResolverMock).userAlias
      )
      IntegrationConfig.testRunner.context.responses.put(
        "port",
        webApplicationContext.webServer.port
      )
    }
  }

  static ServerConfig boot() {
    application = new SpringApplication(Class.forName(System.getenv("SPRING_MAIN_CLASS")))
    application.setAdditionalProfiles('test')
    webApplicationContext = (ServletWebServerApplicationContext) application.run()

    new ServerConfig(
      baseUrl: "http://localhost:${webApplicationContext.webServer.port}",
      types: webApplicationContext.getBeansOfType(SpringModelService).values().collect { it.getType() }
    )
  }
}
