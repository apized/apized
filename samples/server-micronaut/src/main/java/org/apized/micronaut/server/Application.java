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

package org.apized.micronaut.server;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
  info = @Info(
    title = "apized - Micronaut Sample",
    version = "${api.version}",
    description = "${api.description}",
    license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0"),
    contact = @Contact(url = "https://www.apized.org", name = "apized", email = "luis@apized.org")
  )
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class Application {
  public static void main(String[] args) {
    Micronaut
      .build(args)
      .eagerInitSingletons(true)
      .mainClass(Application.class)
      .start();
  }
}
