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

package org.apized.micronaut.server


import org.apized.micronaut.server.api.employee.EmployeeRepository
import org.apized.micronaut.server.api.employee.EmployeeService
import org.apized.micronaut.server.api.employee.Employee
import spock.lang.Specification

class ServiceSpec extends Specification {

  EmployeeService service

  void setup() {
    service = new EmployeeService()
    service.repository = Mock(EmployeeRepository) {
      get(_ as UUID) >> Optional.ofNullable(new Employee())
    }
  }

  void "serv"() {
    when:
    Employee employee = service.get(UUID.randomUUID())

    then:
    employee != null
  }
}
