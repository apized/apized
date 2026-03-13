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

import jakarta.inject.Inject
import org.apized.core.spring.ApizedStartupEvent
import org.apized.test.integration.AbstractTestController
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

class SpringTestController extends AbstractTestController {
  @Inject
  ApplicationEventPublisher publisher

  @Override
  void clear() {
    super.clear()
    publisher.publishEvent(new ApizedStartupEvent(config))
  }

  @RequestMapping(method = RequestMethod.DELETE)
  ResponseEntity reset() {
    super.clear()
    ResponseEntity.ok().build()
  }

  @RequestMapping(path = '/users/{alias}', method = RequestMethod.GET)
  ResponseEntity<String> getUserTokenFor(@PathVariable("alias") String alias) {
    ResponseEntity.ok(super.getTokenFor(alias))
  }

  @RequestMapping(path = '/mocks/{mock}/expectations/{method}', method = RequestMethod.POST)
  ResponseEntity addMockExpectation(@PathVariable("mock") String mock, @PathVariable("method") String method, @RequestBody String expectation) {
    addExpectation(mock, method, expectation)
    ResponseEntity.ok().build()
  }

  @RequestMapping(path = '/mocks/{mock}/executions/{method}', method = RequestMethod.GET)
  ResponseEntity<List<Map>> getMockExecutions(@PathVariable("mock") String mock, @PathVariable("method") String method) {
    ResponseEntity.ok(getExecutions(mock, method))
  }

  @RequestMapping(path = '/mocks/{mock}/executions', method = RequestMethod.DELETE)
  ResponseEntity clearMockExecutions(@PathVariable("mock") String mock) {
    clearExecutions(mock)
    ResponseEntity.ok().build()
  }
}
