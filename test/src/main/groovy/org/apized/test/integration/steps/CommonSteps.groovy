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


import groovy.json.JsonSlurper
import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.BeforeAll
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.apized.test.integration.core.IntegrationConfig
import org.apized.test.integration.core.IntegrationContext
import org.apized.test.integration.core.TestRunner
import org.junit.Assume

class CommonSteps extends AbstractSteps {
  @BeforeAll
  static void setup() {
    testRunner = IntegrationConfig.getTestRunner()
    context = testRunner.context
  }

  @Before
  void step(Scenario scenario) {
    testRunner.clearData()
    if (scenario.sourceTagNames && !scenario.sourceTagNames.contains(testRunner.getTag())) {
      Assume.assumeTrue("Skipped", false)
    }
  }

  @Given('^I login as ([^\\s]+)$')
  def login(String user) {
    testRunner.setUserAs(user)
  }

  @Given('^the context is$')
  def context(DataTable table) {
    context.ids.putAll(table.asMap(String, String))
  }

  @Given('^the responses are expanded to contain ([^\\s]+)$')
  def addExpanded(String property) {
    context.expand.add(property)
  }

  @Given('^the request contains header ([^\\s]+) with value "(.+)"$')
  def addHeader(String header, String value) {
    testRunner.setHeader(header, context.eval(value) as String)
  }

  // <editor-fold desc="List">
  @Given('^I list the ([^\\s]+)$')
  def list(String type) {
    testRunner.get(context, type, null, null)
  }

  @Given('^I list the ([^\\s]+) as ([^\\s]+)$')
  def list(String type, String alias) {
    testRunner.get(context, type, null, alias)
  }
  // </editor-fold>

  // <editor-fold desc="Create">
  @Given('^I create an? empty ([^\\s]+)$')
  def empty(String type) {
    testRunner.post(context, type, null, [ : ], null)
  }

  @Given('^I create an? ([^\\s]+) with$')
  def create(String type, DataTable table) {
    testRunner.post(context, type, null, generatePayload(table.asMap(String, Object)), null)
  }

  @Given('^I create an? ([^\\s]+) as ([^\\s]+) with$')
  def create(String type, String alias, DataTable table) {
    testRunner.post(context, type, null, generatePayload(table.asMap(String, Object)), alias)
  }
  // </editor-fold>

  // <editor-fold desc="Get">
  @When('^I create an? ([^\\s]+) from (.+)$')
  def create(String type, String filename) {
    Map input = new JsonSlurper().parse(getClass().getResourceAsStream("/payloads/$filename")) as Map
    testRunner.post(context, type, null, generatePayload(input), null)
  }

  @Given('^I get an? ([^\\s]+) with id ([^\\s]+)')
  def get(String type, String id) {
    testRunner.get(context, type, id, null)
  }
  // </editor-fold>

  // <editor-fold desc="Update">
  @Given('^I get an? ([^\\s]+) with id ([^\\s]+) as ([^\\s]+)')
  def get(String type, String id, String alias) {
    testRunner.get(context, type, id, alias)
  }

  @Given('^I update an? ([^\\s]+) with id ([^\\s]+) with$')
  def update(String type, String id, DataTable table) {
    testRunner.put(context, type, id, generatePayload(table.asMap(String, Object)), null)
  }

  @Given('^I update an? ([^\\s]+) with id ([^\\s]+) as ([^\\s]+) with$')
  def update(String type, String id, String alias, DataTable table) {
    testRunner.put(context, type, id, table.asMaps(String, Object).collect { convertTypesFromTable(it as Map<String, Object>) }, alias)
  }
  // </editor-fold>

  // <editor-fold desc="Delete">
  @Given('^I delete an? ([^\\s]+) with id ([^\\s]+)$')
  def deleteById(String type, String id) {
    testRunner.delete(context, type, id, null)
  }

  @Given('^I delete an? ([^\\s]+) with id ([^\\s]+) as ([^\\s]+)')
  def deleteById(String type, String id, String alias) {
    testRunner.delete(context, type, id, alias)
  }
  // </editor-fold>

  //<editor-fold desc="Expectations">
  @Given('^I expect service ([^\\s]+) to respond with$')
  def addExpectation(String mock, DataTable table) {
    (table.asMap(String, String) as Map<String, String>).each {
      testRunner.addExpectation(mock, it.key, context.eval(it.value))
    }
  }
  //</editor-fold>

  //<editor-fold desc="Executions">
  @Given('^I get the executions of ([^\\s]+) from service ([^\\s]+)$')
  def getExecutions(String method, String mock) {
    testRunner.getExecutions(context, mock, method, null)
  }

  @Given('^I get the executions of ([^\\s]+) from service ([^\\s]+) as ([^\\s]+)$')
  def getExecutions(String method, String mock, String alias) {
    testRunner.getExecutions(context, mock, method, alias)
  }

  @Given('^I clear the executions of service ([^\\s]+)$')
  def clearExecutions(String mock) {
    testRunner.clearExecutions(context, mock)
  }
  //</editor-fold>

  //<editor-fold desc="Response Code">
  @Given('^the request succeeds$')
  def success() {
    assert context.latestStatus
  }

  @Given('^the ([^\\s]*) request succeeds$')
  def success(String alias) {
    assert context.statuses[alias]
  }

  @Given('^the request fails$')
  def failure() {
    assert !context.latestStatus
  }

  @Given('^the ([^\\s]*) request fails$')
  def failure(String alias) {
    assert !context.statuses[alias]
  }
  //</editor-fold>

  //<editor-fold desc="Response List count">
  @Then('^the response contains (\\d+) elements?$')
  def responseWithSize(int count) {
    assert (context.lastestResponse as List).size() == count
  }

  @Then('^the ([^\\s]*) response contains (\\d+) elements?$')
  def responseWithSize(String alias, int count) {
    assert (context.responses[alias] as List).size() == count
  }
  //</editor-fold>

  //<editor-fold desc="Response verify field values">
  @Then('^the response element (\\d+) contains$')
  def containsElementWith(int index, DataTable table) {
    assert verifyElementMatches((context.lastestResponse as List)[index] as Map, table.asMap(String, String))
  }

  @Then('^the ([^\\s]*) response element (\\d+) contains$')
  def containsElementWith(String alias, int index, DataTable table) {
    assert verifyElementMatches((context.responses[alias] as List)[index] as Map, table.asMap(String, String))
  }
  //</editor-fold>

  //<editor-fold desc="Response List verify field values">
  @Then('^the response contains element with$')
  def responseContainsElementLike(DataTable table) {
    assert countMatchingElements(context.lastestResponse as List, table.asMap(String, String)) > 0
  }

  @Then('^the ([^\\s]*) response contains element with$')
  def responseContainsElementLike(String alias, DataTable table) {
    assert countMatchingElements(context.responses[alias] as List, table.asMap(String, String)) > 0
  }
  //</editor-fold>

  //<editor-fold desc="Response List verify field values negative">
  @Then('^the response does not contain element with$')
  def responseNotContainsElementLike(DataTable table) {
    assert countMatchingElements(context.lastestResponse as List, table.asMap(String, String)) == 0
  }

  @Then('^the ([^\\s]*) response does not contain element with$')
  def responseNotContainsElementLike(String alias, DataTable table) {
    assert countMatchingElements(context.responses[alias] as List, table.asMap(String, String)) == 0
  }
  //</editor-fold>

  //<editor-fold desc="Response List verify field values (multiple)">
  @Then('^the response contains (\\d+) elements with$')
  def responseNotContainsNElementLike(Integer count, DataTable table) {
    assert countMatchingElements(context.lastestResponse as List, table.asMap(String, String)) == count
  }

  @Then('^the ([^\\s]*) response contains (\\d+) elements with$')
  def responseNotContainsNElementLike(String alias, Integer count, DataTable table) {
    assert countMatchingElements(context.responses[alias] as List, table.asMap(String, String)) == count
  }
  //</editor-fold>

  //<editor-fold desc="Response verify field values">
  @Then('^the response contains$')
  def responseContains(DataTable table) {
    assert verifyElementMatches(context.lastestResponse, table.asMap(String, Object))
  }

  @Then('^the response path "(.+)" contains$')
  def responsePathContains(String path, DataTable table) {
    def object = getInPath(context.lastestResponse, path.split(/\./) as List<String>)
    assert verifyElementMatches(object as Map, table.asMap(String, String))
  }

  @Then('^the response path "(.+)" contains element with$')
  def responsePathContainsElement(String path, DataTable table) {
    def object = getInPath(context.lastestResponse, path.split(/\./) as List<String>) as List
    assert countMatchingElements(object, table.asMap(String, Object))
  }

  @Then('^the response path "(.+)" contains (\\d+) elements?$')
  def responsePathContainsNElements(String path, Integer count) {
    def object = getInPath(context.lastestResponse, path.split(/\./) as List<String>) as List
    assert object.size() == count
  }

  @Then('^the response path "(.+)" contains (\\d+) elements with$')
  def responsePathContainsNElements(String path, Integer count, DataTable table) {
    def object = getInPath(context.lastestResponse, path.split(/\./) as List<String>) as List
    assert countMatchingElements(object, table.asMap(String, String)) == count
  }

  @Then('^the ([^\\s]*) response contains$')
  def responseContains(String alias, DataTable table) {
    assert verifyElementMatches(context.responses[alias] as Map, table.asMap(String, String))
  }

  @Then('^the ([^\\s]*) response path "(.+)" contains$')
  def responsePathContains(String alias, String path, DataTable table) {
    def object = getInPath(context.responses[alias], path.split('.') as List<String>)
    assert verifyElementMatches(object as Map, table.asMap(String, String))
  }

  @Then('^the response matches ([^\\s]+)$')
  def responseMatchesExpectedResponse(String filename) {
    Map<String, Object> input = new JsonSlurper().parse(getClass().getResourceAsStream("/payloads/$filename")) as Map<String, Object>
    assert verifyElementMatches(context.lastestResponse as Map, input)
  }
  //</editor-fold>

}
