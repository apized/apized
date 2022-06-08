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

import org.apized.core.mvc.ModelController
import org.apized.test.integration.core.ServerConfig
import org.apized.test.integration.steps.AbstractBootTestServer
import io.cucumber.java.Before
import io.micronaut.context.ApplicationContext
import io.micronaut.core.reflect.GenericTypeUtils
import io.micronaut.http.server.netty.NettyEmbeddedServer
import io.micronaut.runtime.server.EmbeddedServer

class MicronautBootTestServer extends AbstractBootTestServer {

  @Before(order = 1)
  @Override
  void setup() { super.setup() }

  @Override
  ServerConfig boot() {
    EmbeddedServer application = ApplicationContext
      .builder('test')
      .eagerInitSingletons(true)
      .run(NettyEmbeddedServer)
    int port = application.port
    List<Class> types = application.applicationContext.getBeansOfType(ModelController).collect {
      GenericTypeUtils.resolveSuperGenericTypeArgument(it.class.superclass).get()
    }
    new ServerConfig(port: port, types: types)
  }
}
