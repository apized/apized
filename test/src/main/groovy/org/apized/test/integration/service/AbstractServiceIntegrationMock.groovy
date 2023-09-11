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

package org.apized.test.integration.service

import groovy.text.GStringTemplateEngine
import io.micronaut.core.type.Argument
import io.micronaut.serde.ObjectMapper
import org.apized.core.context.ApizedContext
import org.apized.core.security.model.User

import java.util.function.Supplier

abstract class AbstractServiceIntegrationMock implements ServiceIntegrationMock {
  ObjectMapper mapper
  Map<String, String> expectations = [ : ]
  Map<String, List<Map>> executions = [ : ]

  AbstractServiceIntegrationMock(ObjectMapper mapper) {
    this.mapper = mapper
  }

  <T> T execute(String method, Map input, Class<T> clazz, Supplier<T> defaultAction = () -> null) {
    String expected = executeString(method, input)
    if (expected != null) {
      User current = ApizedContext.security.user
      try {
        ApizedContext.security.user = new User(UUID.randomUUID(), 'admin@apized.org', 'administrator', [ ], [ '*' ], [ ])
        mapper.readValue(expected, clazz)
      } finally {
        ApizedContext.security.user = current
      }
    } else {
      defaultAction()
    }
  }

  <T> T execute(String method, Map input, Argument<T> clazz, Supplier<T> defaultAction = () -> null) {
    String expected = executeString(method, input)
    if (expected != null) {
      User current = ApizedContext.security.user
      try {
        ApizedContext.security.user = new User(UUID.randomUUID(), 'admin@apized.org', 'administrator', [ ], [ '*' ], [ ])
        mapper.readValue(expected, clazz)
      } finally {
        ApizedContext.security.user = current
      }
    } else {
      defaultAction()
    }
  }

  private String executeString(String method, Map input) {
    addExecution(method, input)
    getInterpolatedExpectation(method, input)
  }

  @Override
  void setExpectation(String method, String value) {
    expectations[method] = value
  }

  @Override
  String getExpectation(String method) {
    expectations.get(method, null)
  }

  String getInterpolatedExpectation(String method, Map input) {
    if (expectations.containsKey(method)) {
      new GStringTemplateEngine()
        .createTemplate(expectations.get(method, null))
        .make(input)
        .toString()
    } else {
      null
    }
  }

  @Override
  void addExecution(String method, Map arguments) {
    if (!executions.containsKey(method)) {
      executions[method] = [ ]
    }

    executions[method].add(arguments)
  }

  @Override
  List<Map> getExecutions(String method) {
    executions.get(method, [ ])
  }

  @Override
  void clear() {
    expectations = [ : ]
    executions = [ : ]
  }
}
