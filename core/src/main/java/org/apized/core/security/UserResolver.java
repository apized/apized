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

package org.apized.core.security;

import org.apized.core.context.ApizedContext;
import org.apized.core.security.model.User;

import java.util.UUID;

public interface UserResolver {
  User getUser(String token);

  User getUser(UUID userId);

  String generateToken(User user, boolean expiring);

  default void runAs(UUID userId, Runnable execution) {
    if (userId.equals(ApizedContext.getSecurity().getUser().getId())) {
      execution.run();
    } else {
      runAs(getUser(userId), execution);
    }
  }

  default void runAs(String token, Runnable execution) {
    runAs(getUser(token), execution);
  }

  default void runAs(User user, Runnable execution) {
    User currentUser = ApizedContext.getSecurity().getUser();
    try {
      ApizedContext.getSecurity().setUser(user);
      execution.run();
    } finally {
      ApizedContext.getSecurity().setUser(currentUser);
    }
  }
}
