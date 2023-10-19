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

package org.apized.test.integration.mocks


import org.apized.core.StringHelper
import org.apized.core.security.UserResolver
import org.apized.core.security.model.User

abstract class AbstractUserResolverMock implements UserResolver {
  Map<String, User> userAlias = [ : ]
  Map<UUID, User> users = [ : ]

  abstract Map<String, User> getKnownUsers()

  String getTokenForAlias(String user) {
    userAlias.getOrDefault(
      user,
      new User(id: UUID.randomUUID())
    ).getId()
  }

  @Override
  User getUser(String token) {
    users.get(StringHelper.convertStringToUUID(token))
  }

  @Override
  User getUser(UUID userId) {
    Optional.ofNullable(
      users.get(userId)
    ).orElse(
      new User(id: userId, name: "Anonymous user", username: "anonymous@apized.com")
    )
  }

  @Override
  String generateToken(User user, boolean expiring) {
    ''
  }
}
