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

package org.apized.micronaut.security;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.security.AbstractCheckPermissionBehaviour;
import org.apized.core.security.UserResolver;
import org.apized.core.security.enricher.PermissionEnricherManager;

@Context
@Requires(bean = UserResolver.class)
public class CheckPermissionBehaviour extends AbstractCheckPermissionBehaviour {
  @Inject
  ApplicationContext appContext;

  public CheckPermissionBehaviour(ApizedConfig config, BehaviourManager behaviourManager, PermissionEnricherManager enricherManager) {
    super(config.getSlug(), behaviourManager, enricherManager);
  }
}
