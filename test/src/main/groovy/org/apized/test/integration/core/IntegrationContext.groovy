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

import groovy.json.JsonSlurper
import groovy.text.GStringTemplateEngine

class IntegrationContext {
  List<String> expand = [ ]
  Map<String, String> ids = [ : ]
  Map<String, Boolean> statuses = [ : ]
  Map<String, Object> responses = [ : ]
  Boolean latestStatus
  Object lastestResponse

  Object eval(String text) {
    def interpolated = new GStringTemplateEngine()
      .createTemplate(text)
      .make(responses)
      .toString()

    if (interpolated.startsWith('[')) {
      Eval.me(interpolated)
    } else if (interpolated != 'null') {
      interpolated
    } else {
      text
    }
  }

  void addResponse(String type, Boolean success, String body, String alias) {
    latestStatus = success
    lastestResponse = body ? new JsonSlurper().parseText(body) : null
    if (alias) {
      statuses[alias] = latestStatus
      responses[alias] = lastestResponse
    }
    ids[type] = lastestResponse?.id as String
  }

  void clear() {
    expand = [ ]
  }
}
