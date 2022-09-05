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

package org.apized.micronaut.test.integration

import io.cucumber.java.BeforeAll
import io.micronaut.context.ApplicationContext
import io.micronaut.core.reflect.GenericTypeUtils
import io.micronaut.http.server.netty.NettyEmbeddedServer
import io.micronaut.runtime.server.EmbeddedServer
import org.apized.core.mvc.ModelService
import org.apized.test.integration.core.IntegrationConfig
import org.apized.test.integration.core.IntegrationContext
import org.apized.test.integration.core.RestIntegrationTest
import org.apized.test.integration.core.ServerConfig
import org.apized.test.integration.mocks.AbstractUserResolverMock

class MicronautBootTestServer {
  static boolean initialized = false
  static private EmbeddedServer application

  @BeforeAll(order = 1)
  static void setup() {
    if (!initialized) {
      initialized = true
      IntegrationConfig.setTestRunner(new RestIntegrationTest(new IntegrationContext()), boot())
      IntegrationConfig.testRunner.context.responses.put(
        "users",
        application.applicationContext.getBean(AbstractUserResolverMock).userAlias
      )
    }
  }

  static ServerConfig boot() {
    application = ApplicationContext
      .builder()
      .eagerInitSingletons(true)
      .run(NettyEmbeddedServer)
    int port = application.port
    List<Class> types = application.applicationContext.getBeansOfType(ModelService).collect {
      GenericTypeUtils.resolveSuperGenericTypeArgument(it.class.superclass).get()
    }
    new ServerConfig(port: port, types: types)
  }
}
