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

package org.apized.spring.server.behaviour;

import jakarta.inject.Inject;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;
import org.apized.spring.annotation.SpringBehaviourExecution;
import org.apized.spring.annotation.SpringBulkBehaviourExecution;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({ "unchecked" })
@Aspect
@Component
public class BulkBehaviourExecutor {
  @Inject
  BehaviourManager manager;

  @Around("@annotation(org.apized.spring.annotation.SpringBulkBehaviourExecution)")
  public List<Model> intercept(ProceedingJoinPoint call) throws Throwable {
    MethodSignature signature = (MethodSignature) call.getSignature();
    Method method = signature.getMethod();

    var execution = method.getAnnotation(SpringBulkBehaviourExecution.class).execution();

    Class<? extends Model> model = execution.model();
    Layer layer = execution.layer();
    Action action = execution.action();

    List<Model> inputs = (List<Model>) Arrays.stream(call.getArgs()).findFirst().orElse(null);

    inputs.forEach(input ->
      manager.executeBehavioursFor(
        model,
        layer,
        When.BEFORE,
        action,
        Execution.builder().id(input.getId()).input(input).build()
      )
    );

    List<Model> result = (List<Model>) call.proceed();

    for (int i = 0; i < inputs.size(); i++) {
      manager.executeBehavioursFor(
        model,
        layer,
        When.AFTER,
        action,
        Execution.builder().id(inputs.get(i).getId()).input(inputs.get(i)).output(result.get(i)).build()
      );
    }

    return result;
  }
}
