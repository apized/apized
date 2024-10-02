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

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.apized.core.ApizedConfig
import org.apized.core.Dialect
import org.apized.core.error.exception.BadRequestException
import org.apized.test.integration.mocks.AbstractUserResolverMock
import org.apized.test.integration.service.ServiceIntegrationMock

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

@Slf4j
abstract class AbstractTestController {

  @Inject
  abstract List<ServiceIntegrationMock> mocks

  @Inject
  DataSource dataSource

  @Inject
  ApizedConfig config

  @Inject
  AbstractUserResolverMock userResolverMock

  void clear() {
    clearDB()
    mocks.each {
      it.clear()
    }
  }

  void clearDB() {
    switch (config.dialect) {
      case Dialect.H2:
        clearH2DB()
        break
      case Dialect.MYSQL:
        clearMySQLDB()
        break
      case Dialect.POSTGRES:
        clearPostgresDB()
        break
      case Dialect.ORACLE:
        clearOracleDB()
        break
      case Dialect.SQL_SERVER:
        clearSqlServerDB()
        break
      default: throw new BadRequestException("Please provide the implementation for clearDB")
    }
  }

  void clearSqlServerDB() {
    List<String> tables = [ ]
    Connection connection = dataSource.getConnection()
    ResultSet resultSet = connection.prepareStatement("select table_name from information_schema.tables where table_type = 'BASE TABLE'").executeQuery()
    while (resultSet.next()) {
      String table = resultSet.getString(resultSet.findColumn('table_name'))
      if (!table.toLowerCase().startsWith('flyway')) {
        tables.add(table)
      }
    }

    tables.each {
      connection.prepareStatement("alter table $it NOCHECK CONSTRAINT ALL").execute()
    }
    tables.each {
      connection.prepareStatement("delete from $it").execute()
    }
    tables.each {
      connection.prepareStatement("alter table $it CHECK CONSTRAINT ALL").execute()
    }

    connection.close()
  }

  void clearOracleDB() {
    Connection connection = dataSource.getConnection()

    Map<String, String> constraints = [ : ]
    ResultSet constraintSet = connection.prepareStatement("select constraint_name, table_name from user_constraints where constraint_type='R'").executeQuery();
    while (constraintSet.next()) {
      constraints.put(
        constraintSet.getString(constraintSet.findColumn('constraint_name')),
        constraintSet.getString(constraintSet.findColumn('table_name'))
      )
    }

    constraints.each {
      connection.prepareStatement("alter table $it.value disable constraint $it.key").execute()
    }

    ResultSet resultSet = connection.prepareStatement("select table_name from user_tables").executeQuery()
    while (resultSet.next()) {
      String table = resultSet.getString(resultSet.findColumn('table_name'))
      if (!table.toLowerCase().startsWith('flyway')) {


        connection.prepareStatement("truncate table $table cascade").execute()
      }
    }

    constraints.each {
      connection.prepareStatement("alter table $it.value enable constraint $it.key").execute()
    }

    connection.close()
  }

  void clearMySQLDB() {
    Connection connection = dataSource.getConnection()
    connection.prepareStatement("SET foreign_key_checks = 0").execute()
    ResultSet resultSet = connection.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' and table_schema = 'test'").executeQuery()
    while (resultSet.next()) {
      String table = resultSet.getString(resultSet.findColumn('table_name'))
      if (!table.toLowerCase().startsWith('flyway')) {
        connection.prepareStatement("truncate table $table").execute()
      }
    }
    connection.prepareStatement("SET foreign_key_checks = 1").execute()
    connection.close()
  }

  void clearH2DB() {
    Connection connection = dataSource.getConnection()
    connection.prepareStatement("SET REFERENTIAL_INTEGRITY FALSE").execute()
    ResultSet resultSet = connection.prepareStatement("show tables").executeQuery()
    while (resultSet.next()) {
      String table = resultSet.getString(resultSet.findColumn('table_name'))
      if (!table.toLowerCase().startsWith('flyway')) {
        connection.prepareStatement("truncate table $table").execute()
      }
    }
    connection.prepareStatement("SET REFERENTIAL_INTEGRITY TRUE").execute()
    connection.close()
  }

  void clearPostgresDB() {
    Connection connection = dataSource.getConnection()
    ResultSet resultSet = connection.prepareStatement("select tablename from pg_tables where schemaname='public'").executeQuery()
    while (resultSet.next()) {
      String table = resultSet.getString(resultSet.findColumn('tablename'))
      if (!table.toLowerCase().startsWith('flyway')) {
        connection.prepareStatement("truncate table \"$table\" cascade").execute()
      }
    }
    connection.close()
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

  void clearExecutions(String mock) {
    mocks
      .find { it.mockedServiceName == mock }
      .clear()
  }
}
