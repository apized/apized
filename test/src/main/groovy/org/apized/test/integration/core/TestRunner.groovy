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

import io.restassured.specification.RequestSpecification

interface TestRunner {
  IntegrationContext getContext()

  String getTag()

  RequestSpecification getClient()

  RequestSpecification getClient(IntegrationContext context)

  RequestSpecification getClient(String user)

  RequestSpecification getClient(IntegrationContext context, String user)

  void clearData()

  void addExpectation(String mock, String method, Object value)

  void getExecutions(IntegrationContext context, String mock, String method, String alias)

  void get(IntegrationContext context, String type, String id, String alias)

  void post(IntegrationContext context, String type, String id, Object payload, String alias)

  void put(IntegrationContext context, String type, String id, Object payload, String alias)

  void delete(IntegrationContext context, String type, String id, String alias)

  String getUserId(String user)

  void setUserAs(String user)

  String getToken()

  void setToken(String token)

  void setHeader(String header, String value)
}
