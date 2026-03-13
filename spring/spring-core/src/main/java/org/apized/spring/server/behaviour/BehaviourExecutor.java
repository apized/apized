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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;


@Aspect
@Component
public class BehaviourExecutor {
  @Inject
  BehaviourManager manager;

  @Around("@annotation(org.apized.spring.annotation.SpringBehaviourExecution)")
  public Object intercept(ProceedingJoinPoint call) throws Throwable {
    MethodSignature signature = (MethodSignature) call.getSignature();
    Method method = signature.getMethod();

    var execution = method.getAnnotation(SpringBehaviourExecution.class).execution();

    Class<? extends Model> model = execution.model();
    Layer layer = execution.layer();
    Action action = execution.action();

    UUID id = (UUID) Arrays.stream(call.getArgs()).filter(v -> v instanceof UUID).findFirst().orElse(null);
    Model input = (Model) Arrays.stream(call.getArgs()).filter(v -> v instanceof Model).findFirst().orElse(null);

    manager.executeBehavioursFor(
      model,
      layer,
      When.BEFORE,
      action,
      Execution.builder().id(id).input(input).build()
    );

    Model result = (Model) call.proceed();

    manager.executeBehavioursFor(
      model,
      layer,
      When.AFTER,
      action,
      Execution.builder().id(id).input(input).output(result).build()
    );

    return result;
  }
}
