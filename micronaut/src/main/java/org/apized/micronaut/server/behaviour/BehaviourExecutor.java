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

import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.behaviour.annotation.BehaviourExecution;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;
import org.apized.micronaut.annotation.MicronautBehaviourExecution;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
@Singleton
@InterceptorBean(MicronautBehaviourExecution.class)
public class BehaviourExecutor implements MethodInterceptor<Object, Model> {
  @Inject
  BehaviourManager manager;

  @Override
  public Model intercept(MethodInvocationContext<Object, Model> context) {
    AnnotationValue<BehaviourExecution> execution = context.getAnnotationMetadata()
      .findAnnotation(MicronautBehaviourExecution.class).get()
      .getAnnotation("execution", BehaviourExecution.class).get();

    Class<? extends Model> model = (Class<? extends Model>) execution.classValue("model").get();
    Layer layer = execution.enumValue("layer", Layer.class).get();
    Action action = execution.enumValue("action", Action.class).get();

    manager.executeBehavioursFor(
      model,
      layer,
      When.BEFORE,
      action,
      Execution.builder().inputs(context.getParameterValueMap()).build()
    );

    Model result = context.proceed();

    manager.executeBehavioursFor(
      model,
      layer,
      When.AFTER,
      action,
      Execution.builder().inputs(context.getParameterValueMap()).output(result).build()
    );

    return result;
  }
}
