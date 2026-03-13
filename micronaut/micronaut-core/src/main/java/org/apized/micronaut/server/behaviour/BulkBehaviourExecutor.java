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

package org.apized.micronaut.server.behaviour;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.behaviour.annotation.BehaviourExecution;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;
import org.apized.micronaut.annotation.MicronautBehaviourExecution;
import org.apized.micronaut.annotation.MicronautBulkBehaviourExecution;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
@Singleton
@InterceptorBean(MicronautBulkBehaviourExecution.class)
public class BulkBehaviourExecutor implements MethodInterceptor<Object, List<Model>> {
  @Inject
  BehaviourManager manager;

  @Override
  public List<Model> intercept(MethodInvocationContext<Object, List<Model>> context) {
    AnnotationValue<BehaviourExecution> execution = context.getAnnotationMetadata()
      .findAnnotation(MicronautBulkBehaviourExecution.class).get()
      .getAnnotation("execution", BehaviourExecution.class).get();

    Class<? extends Model> model = (Class<? extends Model>) execution.classValue("model").get();
    Layer layer = execution.enumValue("layer", Layer.class).get();
    Action action = execution.enumValue("action", Action.class).get();

    Map<String, Object> parameterValueMap = context.getParameterValueMap();
    List<Model> inputs = (List<Model>) parameterValueMap.values().stream().findFirst().orElse(null);

    inputs.forEach(input ->
      manager.executeBehavioursFor(
        model,
        layer,
        When.BEFORE,
        action,
        Execution.builder().id(input.getId()).input(input).build()
      )
    );

    List<Model> result = context.proceed();

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
