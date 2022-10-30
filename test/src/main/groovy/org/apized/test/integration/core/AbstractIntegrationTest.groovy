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


import io.restassured.http.ContentType
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

import static io.restassured.RestAssured.given
import static io.restassured.config.RestAssuredConfig.newConfig

abstract class AbstractIntegrationTest implements TestRunner {
  private IntegrationContext context

  String token
  Map<String, String> headers = [ : ]
  Map<String, String> users = [ : ]

  AbstractIntegrationTest(IntegrationContext context) {
    this.context = context
  }

  @Override
  IntegrationContext getContext() {
    context
  }

  @Override
  void addExpectation(String mock, String method, Object value) {
    getClient().body(value).post("/integration/mocks/${mock}/expectations/${method}")
  }

  @Override
  void getExecutions(IntegrationContext context, String mock, String method, String alias) {
    Response response = getClient().get("/integration/mocks/${mock}/executions/${method}")
    context.addResponse('execution', response.statusCode() == 200, response.asString(), alias)
  }

  @Override
  void clearExecutions(IntegrationContext context, String mock, String alias) {
    Response response = getClient().delete("/integration/mocks/${mock}/executions")
    context.addResponse('execution', response.statusCode() == 200, response.asString(), alias)
  }

  @Override
  void clearData() {
    getClient().delete("/integration")
    headers.clear()
    users.clear()
    context.clear()
    token = null
  }

  void ensureUser(String user) {
    if (!users.containsKey(user)) {
      Response response = getClient().get("/integration/users/${user}")
      users[user] = response.asString()
    }
  }

  @Override
  String getUserId(String user) {
    ensureUser(user)
    users[user]
  }

  String getUserToken(String user) {
    ensureUser(user)
    users[user]
  }

  @Override
  void setUserAs(String user) {
    token = getUserToken(user)
  }

  @Override
  void setHeader(String header, String value) {
    if (value != null) {
      headers.put(header, value)
    } else {
      headers.remove(header)
    }
  }

  @Override
  RequestSpecification getClient(String user) {
    getClient(null, user)
  }

  @Override
  @SuppressWarnings('GrMethodMayBeStatic')
  RequestSpecification getClient(IntegrationContext context = null, String user = null) {
    def client = given()
      .contentType(ContentType.JSON)
      .baseUri(IntegrationConfig.getInstance().baseUrl)
      .config(newConfig())
      .header('Content-Type', 'application/json')

    if (headers) {
      headers.each {
        client.header(it.key, it.value)
      }
    }
    if (context?.expand) {
      client = client.queryParam('fields', "*")
      context.expand.each {
        client = client.queryParam('fields', "$it.*")
      }
    }

    if (token || user) {
      client.header('X-DEBUG-User', user ?: users.find { it.value == token }.key)
      client.header('Authorization', "Bearer ${user ? getUserToken(user) : token}")
    }

    client = client
      .given()
      .filter(new TestClientLogFilter(getTag()))

    client
  }
}
