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

package org.apized.spring.server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

@OpenAPIDefinition(
  info = @Info(
    title = "apized - Spring Sample",
    version = "${api.version}",
    license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0"),
    contact = @Contact(url = "https://apized.org", name = "apized")
  )
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@ComponentScan(basePackages = "org.apized")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
