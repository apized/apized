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

import org.apized.core.StringHelper
import org.apized.core.model.Apized
import groovy.json.JsonOutput
import io.restassured.response.Response

class RestIntegrationTest extends AbstractIntegrationTest {
  RestIntegrationTest(IntegrationContext context) {
    super(context)
  }

  @Override
  String getTag() {
    "@rest"
  }

  @Override
  void get(IntegrationContext context, String type, String id, String alias) {
    Response response = getClient(context).get(context.eval(getPathFor(context, id ? type : StringHelper.singularize(type), id)) as String)
    context.addResponse(type, (response.statusCode() / 100 as int) == 2, response.asString(), alias)
  }

  @Override
  void post(IntegrationContext context, String type, String id, Object payload, String alias) {
    Response response = getClient(context).body(JsonOutput.toJson(payload)).post(context.eval(getPathFor(context, type, id)) as String)
    context.addResponse(type, (response.statusCode() / 100 as int) == 2, response.asString(), alias)
  }

  @Override
  void put(IntegrationContext context, String type, String id, Object payload, String alias) {
    def response = getClient(context).body(JsonOutput.toJson(payload)).put(context.eval(getPathFor(context, type, id)) as String)
    context.addResponse(type, (response.statusCode() / 100 as int) == 2, response.asString(), alias)
  }

  @Override
  void delete(IntegrationContext context, String type, String id, String alias) {
    Response response = getClient(context).delete(context.eval(getPathFor(context, type, id)) as String)
    context.addResponse(type, (response.statusCode() / 100 as int) == 2, response.asString(), alias)
  }

  @SuppressWarnings('GrMethodMayBeStatic')
  String getPathFor(IntegrationContext context, String type, String id = null) {
    def types = IntegrationConfig.getInstance().getTypes()
    def clazz = types.find { it.simpleName.toLowerCase() == type.toLowerCase() }
    if (!clazz) {
      throw new Exception("Invalid type $type.\n Known types are: ${types.join(", ")}")
    }

    List<String> scopes = [ ]
    //noinspection GroovyUnusedAssignment
    Class scope = clazz
    while (scope) {
      Class[] subScopes = scope.getAnnotation(Apized).scope()
      scope = subScopes.length ? subScopes[0] : null
      if (scope) {
        scopes.add(scope.simpleName.toLowerCase())
      }
    }

    String path = ""
    scopes.reverse().each {
      path += "/${StringHelper.pluralize(it)}/${context.ids[it]}"
    }

    path += "/${StringHelper.pluralize(type)}"

    if (id) {
      path += "/$id"
    }

    path
  }
}
