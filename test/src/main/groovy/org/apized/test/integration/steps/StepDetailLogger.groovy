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


import groovy.util.logging.Slf4j
import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.*
import org.apized.test.integration.core.LogUtils

@Slf4j
class StepDetailLogger implements ConcurrentEventListener {

  public EventHandler<TestStepStarted> testStepStartedHandler = { TestStepStarted event -> testStepStarted(event) }
  public EventHandler<TestStepFinished> testStepFinishedHandler = { TestStepFinished event -> testStepFinished(event) }

  @Override
  void setEventPublisher(EventPublisher publisher) {
    publisher.registerHandlerFor(TestStepStarted.class, testStepStartedHandler)
    publisher.registerHandlerFor(TestStepFinished.class, testStepFinishedHandler)
  }

  private static void testStepStarted(TestStepStarted event) {
    if (event.getTestStep() instanceof PickleStepTestStep) {
      logPickleStep("Starting", (PickleStepTestStep) event.getTestStep())
    } else if (event.getTestStep() instanceof HookTestStep) {
      HookTestStep testStep = (HookTestStep) event.getTestStep()
      logHookStep("Starting", testStep)
    }
  }

  private static void logPickleStep(String action, PickleStepTestStep testStep) {
    String stepName = testStep.getStep().getText()
    String fileUri = testStep.getUri().toASCIIString()
    String fileLink = fileUri + ":" + testStep.getStep().getLine()
    String startHeader = action + " '" + stepName + "'"
    LogUtils.logLongHeader(log, startHeader, fileLink)
  }

  private static void logHookStep(String action, HookTestStep testStep) {
    String hookName = action + " " + testStep.getHookType().toString() + " Hook"
    String fileLink = testStep.getCodeLocation()
    LogUtils.logLongHeader(log, hookName, fileLink)
  }

  private static void testStepFinished(TestStepFinished event) {
    String status = event.getResult().getStatus().toString()
    String tag = "Finished with " + status + ":"
    if (event.getTestStep() instanceof PickleStepTestStep) {
      logPickleStep(tag, (PickleStepTestStep) event.getTestStep())
    } else if (event.getTestStep() instanceof HookTestStep) {
      HookTestStep testStep = (HookTestStep) event.getTestStep()
      logHookStep(tag, testStep)
    }
  }
}
