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

abstract class AbstractServiceIntegrationMock implements ServiceIntegrationMock {
  Map<String, String> expectations = [ : ]
  Map<String, List<Map>> executions = [ : ]

  @Override
  void setExpectation(String method, String value) {
    expectations[method] = value
  }

  @Override
  String getExpectation(String method) {
    expectations.get(method, null)
  }

  String getInterpolatedExpectation(String method, Map input) {
    new GStringTemplateEngine()
      .createTemplate(expectations.get(method, null))
      .make(input)
      .toString()
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
