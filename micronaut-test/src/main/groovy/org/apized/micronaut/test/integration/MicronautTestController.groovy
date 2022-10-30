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


import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import org.apized.test.integration.AbstractTestController

class MicronautTestController extends AbstractTestController {
  @Delete
  HttpResponse reset() {
    super.clear()
    HttpResponse.status(HttpStatus.OK)
  }

  @Get('/users/{alias}')
  HttpResponse<String> getUserTokenFor(@PathVariable("alias") String alias) {
    HttpResponse.status(HttpStatus.OK).body(super.getTokenFor(alias))
  }

  @Post('/mocks/{mock}/expectations/{method}')
  HttpResponse addMockExpectation(@PathVariable("mock") String mock, @PathVariable("method") String method, @Body String expectation) {
    addExpectation(mock, method, expectation)
    HttpResponse.status(HttpStatus.OK)
  }

  @Get('/mocks/{mock}/executions/{method}')
  HttpResponse<List<Map>> getMockExecutions(@PathVariable("mock") String mock, @PathVariable("method") String method) {
    HttpResponse.status(HttpStatus.OK).body(getExecutions(mock, method))
  }

  @Delete('/mocks/{mock}/executions')
  HttpResponse clearMockExecutions(@PathVariable("mock") String mock) {
    clearExecutions(mock)
    HttpResponse.status(HttpStatus.OK)
  }
}
