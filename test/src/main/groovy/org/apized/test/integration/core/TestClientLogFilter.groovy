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

import static io.restassured.filter.log.LogDetail.ALL

@Slf4j
class TestClientLogFilter implements Filter {

  private final String clientTag
  private final LogDetail logDetail = ALL
  private final PrintStream requestStream
  private final PrintStream responseStream
  private final boolean shouldPrettyPrint = true

  /**
   * Little utility class to convert each request/response call into
   * its own log call to ensure they go through the same buffer
   * and are flushed in the correct order
   */
  protected class LogPrintStream extends PrintStream {
    Logger log
    String tag

    LogPrintStream(Logger log, String tag) {
      super(System.out)
      this.log = log
      this.tag = tag
    }

    @Override
    void print(String s) {

      log.info(
        LogUtils.formatHeaderCenter("RAW HTTP " + tag + ":") + "\n\n" +
          LogUtils.formatHeaderCenter("RAW HTTP " + tag + " START") + "\n\n" +
          s + "\n\n" +
          LogUtils.formatHeaderCenter("RAW HTTP " + tag + " END"))
    }
  }

  TestClientLogFilter(String clientTag) {
    this.clientTag = clientTag
    this.requestStream = new LogPrintStream(log, "REQUEST")
    this.responseStream = new LogPrintStream(log, "RESPONSE")
  }

  Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
    // Print request before it heads out
    logRequest(requestSpec)
    String uri = requestSpec.getURI()
    RequestPrinter.print(requestSpec, requestSpec.getMethod(), uri, logDetail, new HashSet<>(), requestStream, shouldPrettyPrint)

    // Send request and collect response through filter chain
    Response response = ctx.next(requestSpec, responseSpec)

    // print response as it arrived
    logResponse(requestSpec, response)
    ResponsePrinter.print(response, response, responseStream, logDetail, shouldPrettyPrint, new HashSet<>())

    // Ensure correct handling of outer filter layers
    final byte[] responseBody = response.asByteArray()
    response = cloneResponseIfNeeded(response, responseBody)
    return response
  }

  protected void logRequest(FilterableRequestSpecification requestSpec) {
    String statusLine = clientTag + " " + requestSpec.getMethod() + " Request to:"
    LogUtils.logHeader(log, statusLine, requestSpec.getUserDefinedPath())
  }

  protected void logResponse(FilterableRequestSpecification requestSpec, Response response) {
    String statusLine = clientTag + " " + requestSpec.getMethod() + " Response " + response.statusCode() + " to:"
    LogUtils.logHeader(log, statusLine, requestSpec.getUserDefinedPath())
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
