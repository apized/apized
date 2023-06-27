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

import groovy.util.logging.Slf4j
import io.restassured.builder.ResponseBuilder
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.filter.log.LogDetail
import io.restassured.internal.RestAssuredResponseImpl
import io.restassured.internal.print.RequestPrinter
import io.restassured.internal.print.ResponsePrinter
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import org.slf4j.Logger

@Slf4j
class TestClientLogFilter implements Filter {

  private final PrintStream requestStream
  private final PrintStream responseStream
  /**
   * Little utility class to convert each request/response call into
   * its own log call to ensure they go through the same buffer
   * and are flushed in the correct order
   */
  protected class LogPrintStream extends PrintStream {
    Logger log

    LogPrintStream(Logger log) {
      super(new ByteArrayOutputStream())
      this.log = log
    }

    @Override
    void print(String s) {
      log.info("\n{}", s)
    }
  }

  TestClientLogFilter() {
    requestStream = new LogPrintStream(log)
    responseStream = new LogPrintStream(log)
  }

  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    RequestPrinter.print(requestSpec, requestSpec.getMethod(), requestSpec.getURI(), LogDetail.ALL, new HashSet<>(), requestStream, true)

    Response response = ctx.next(requestSpec, responseSpec)

    ResponsePrinter.print(response, response, responseStream, LogDetail.ALL, true, new HashSet<>())

    final byte[] responseBody = response.asByteArray()
    response = cloneResponseIfNeeded(response, responseBody)
    return response
  }

  private static Response cloneResponseIfNeeded(Response response, byte[] responseAsString) {
    if (responseAsString != null && response instanceof RestAssuredResponseImpl && !((RestAssuredResponseImpl) response).getHasExpectations()) {
      final Response build = new ResponseBuilder().clone(response).setBody(responseAsString).build()
      ((RestAssuredResponseImpl) build).setHasExpectations(true)
      return build
    }
    return response
  }
}
