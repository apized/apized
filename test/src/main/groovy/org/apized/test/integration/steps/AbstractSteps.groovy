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

package org.apized.test.integration.steps


import org.apized.test.integration.core.IntegrationContext

import java.util.regex.Pattern

abstract class AbstractSteps {
  protected IntegrationContext context

  @SuppressWarnings(['GroovyAssignabilityCheck', 'GrEqualsBetweenInconvertibleTypes'])
  def verifyElementMatches(Object obj, Map<String, Object> match) {
    boolean matches = true
    match.each {

      def objValue = obj
      objValue = getInPath(objValue, it.key)

      def value = it.value
      if (value instanceof String && !value.startsWith('/')) {
        value = context.eval(value)
      }

      if (objValue instanceof List) {
        matches = matches && value.size() == objValue.size()
        if (matches) {
          objValue.eachWithIndex { el, index ->
            if (el instanceof Map) {
              matches = matches && verifyElementMatches(el, value[index])
            } else {
              matches = matches && verifyPrimitiveMatches(objValue, value)
            }
          }
        }
      } else if (objValue instanceof Map) {
        matches = matches && verifyElementMatches(objValue, value)
      } else {
        matches = matches && verifyPrimitiveMatches(objValue, value)
      }
    }
    matches
  }

  def verifyPrimitiveMatches(Object objValue, Object value) {
    if (value instanceof String) {
      if (value.startsWith('/')) {
        return Pattern.compile(value.substring(1, value.length() - 1), Pattern.DOTALL)
          .matcher(objValue.toString())
          .matches()
      } else {
        value = context.eval(value)
      }
      if (objValue instanceof Integer) {
        value = Integer.parseInt(value as String)
      } else if (objValue instanceof Long) {
        value = Long.parseLong(value as String)
      } else if (objValue instanceof Float) {
        value = Float.parseFloat(value as String)
      } else if (objValue instanceof Double) {
        value = Double.parseDouble(value as String)
      } else if (objValue instanceof Boolean) {
        value = Boolean.parseBoolean(value as String)
      } else if (objValue instanceof BigDecimal) {
        value = new BigDecimal(value as String)
      }
    }

    value == objValue
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def countMatchingElements(List obj, Map match) {
    obj.findAll {
      verifyElementMatches(it, match)
    }.size()
  }

  Map generatePayload(Map<String, Object> map) {
    map.collectEntries {
      def value = it.value

      if (value != null) {
        if (value instanceof List) {
          value = value.collect {
            if (it instanceof String) {
              context.eval(value as String)
            } else {
              generatePayload(it as Map)
            }
          }
        } else if (value instanceof Map) {
          value = generatePayload(value as Map)
        } else if (value instanceof String) {
          value = context.eval(value as String)
        }
      }

      [ (context.eval(it.key)): value ]
    }
  }

  /**
   * Traverses this object by an access path and returns the result
   *
   * @param object the object to traverse
   * @param path the access path, as a dot joined string
   * @return the resulting object at the end of the path
   */
  Object getInPath(Object object, String path) {
    getInPath(object, path.split('\\.').toList())
  }

  /**
   * Traverses this object by an access path and returns the result
   *
   * @param object the object to traverse
   * @param path the access path, as a list of strings
   * @return the resulting object at the end of the path
   */
  Object getInPath(Object object, List<String> path) {
    def property = path.remove(0)
    if (property == '_') {
      return object
    }

    def val
    if (object instanceof Map) {
      val = object[property]
    } else if (object instanceof List) {
      try {
        val = object[Integer.parseInt(property)]
      } catch (ignored) {
        val = object.find { it.id == property }
      }
    } else {
      val = object.metaClass.getProperty(object, property)
    }

    if (path) {
      getInPath(val, path)
    } else {
      val
    }
  }

  Map<String, Object> convertTypesFromTable(Map<String, String> map) {
    map.collectEntries {
      String verifier = it.value.toString().toLowerCase()
      def val

      if (it.value != null) {
        if ((it.value.startsWith("'") && it.value.endsWith("'")) || (it.value.startsWith("'") && it.value.endsWith("'"))) {
          val = context.eval(it.value.substring(1, it.value.length() - 1))
        } else if (verifier.matches(/^(true|false)$/)) {
          val = Boolean.parseBoolean(verifier)
        } else if (verifier.matches(/^\d+\.\d+$/)) {
          val = Double.parseDouble(verifier)
        } else if (verifier.matches(/^\d+$/)) {
          val = Long.parseLong(verifier)
        } else {
          val = context.eval(it.value)
        }
      }

      [ (it.key): val ]
    } as Map<String, Object>
  }
}
