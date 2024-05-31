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

package org.apized.core.behaviour;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.error.exception.ServerException;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Layer;
import org.apized.core.model.Model;
import org.apized.core.model.When;
import org.apized.core.tracing.Traced;

import java.util.*;

@Slf4j
@Singleton
public class BehaviourManager {
  Map<String, List<BehaviourEntry>> registry = new HashMap<>();

  public void registerBehaviour(Class<? extends Model> type, Layer layer, List<When> when, List<Action> actions, Integer order, BehaviourHandler<? extends Model> behaviour) {
    for (When wh : when) {
      for (Action op : actions) {
        String signature = layer.getKey() + ":" + wh.getKey() + ":" + op.getType();
        List<BehaviourEntry> entry = registry.getOrDefault(signature, new ArrayList<>());
        entry.add(new BehaviourEntry(behaviour.getClass().getSimpleName(), type, order, behaviour));
        registry.put(signature, entry);
      }
    }
  }

  public void executeBehavioursFor(Class type, Layer layer, When when, Action action, Execution execution) {
    try {
      String signature = layer.getKey() + ":" + when.getKey() + ":" + action.getType();
      registry.getOrDefault(signature, new ArrayList<>()).stream()
        .filter(b -> b.type().isAssignableFrom(type))
        .sorted(Comparator.comparingInt(BehaviourEntry::order))
        .forEach(it -> {
          long init = System.currentTimeMillis();
          log.info("[" + signature + ":" + it.type().getSimpleName() + "] Executing behaviour " + it.name());
          it.behaviour().process(type, when, action, execution);
          log.info("[" + signature + ":" + it.type().getSimpleName() + "] Behaviour {} done in {}ms", it.name(), System.currentTimeMillis() - init);
        });
    } catch (Exception e) {
      if (!(e instanceof ServerException)) {
        log.error("Error occurred running '" + type + "." + layer.getKey() + "." + when.getKey() + "'", e);
      }
      throw e;
    }
  }
}
