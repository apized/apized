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

import org.apized.core.behaviour.BehaviourManager;
import org.apized.core.security.AbstractCheckPermissionBehaviour;
import org.apized.core.security.UserResolver;
import org.apized.micronaut.core.ApizedConfig;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Singleton
@Requires(bean = UserResolver.class)
public class CheckPermissionBehaviour extends AbstractCheckPermissionBehaviour {

  public CheckPermissionBehaviour(ApizedConfig config, BehaviourManager manager) {
    super(config.getSlug(), manager);
  }
}
