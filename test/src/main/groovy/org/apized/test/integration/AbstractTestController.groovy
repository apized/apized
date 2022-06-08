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

package org.apized.test.integration

import org.apized.test.integration.mocks.AbstractUserResolverMock
import org.apized.test.integration.service.ServiceIntegrationMock
import jakarta.inject.Inject

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

abstract class AbstractTestController {
  @Inject
  abstract List<ServiceIntegrationMock> mocks

  @Inject
  DataSource dataSource

  @Inject
  AbstractUserResolverMock userResolverMock

  void clear() {
    Connection connection = dataSource.getConnection()
    ResultSet resultSet = connection.prepareStatement("select tablename from pg_tables where schemaname='public'").executeQuery()
    while (resultSet.next()) {
      String table = resultSet.getString(resultSet.findColumn('tablename'))
      if (!table.toLowerCase().startsWith('flyway')) {
        Connection subConnection = dataSource.getConnection()
        subConnection.prepareStatement("truncate table $table cascade").execute()
        subConnection.close()
      }
    }
    connection.close()
    mocks.each {
      it.clear()
    }
  }

  String getTokenFor(String alias) {
    userResolverMock.getTokenForAlias(alias)
  }

  void addExpectation(String mock, String method, String expectation) {
    mocks
      .find { it.mockedServiceName == mock }
      .setExpectation(method, expectation)
  }

  List<Map> getExecutions(String mock, String method) {
    mocks
      .find { it.mockedServiceName == mock }
      .getExecutions(method)
  }
}
