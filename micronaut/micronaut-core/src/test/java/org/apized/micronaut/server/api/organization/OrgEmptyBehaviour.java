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

package org.apized.micronaut.server.api.organization;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.behaviour.BehaviourHandler;
import org.apized.core.behaviour.annotation.Behaviour;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.When;

@Slf4j
@Singleton
@Behaviour(
  model = Organization.class,
  layer = Layer.SERVICE,
  when = {When.BEFORE, When.AFTER},
  actions = {Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE},
  order = Integer.MAX_VALUE
)
public class OrgEmptyBehaviour implements BehaviourHandler<Organization> {
}
