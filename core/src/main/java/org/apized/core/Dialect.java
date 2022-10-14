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

package org.apized.core;

/**
 * Enumeration of the possible supported database dialects
 */
public enum Dialect {
  /**
   * H2 database.
   */
  H2,
  /**
   * MySQL 5.5 or above.
   */
  MYSQL,
  /**
   * Postgres 9.5 or later.
   */
  POSTGRES,
  /**
   * SQL server 2012 or above.
   */
  SQL_SERVER,
  /**
   * Oracle 12c or above.
   */
  ORACLE,
  /**
   * Ansi compliant SQL.
   */
  ANSI
}
