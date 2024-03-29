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

import jakarta.inject.Singleton;
import org.apized.core.security.model.User;

import java.util.List;
import java.util.UUID;

@Singleton
public class MemoryUserResolver implements UserResolver {

  User admin = User
    .builder()
    .id(UUID.randomUUID())
    .name("Administrator")
    .username("admin@apized.com")
    .permissions(List.of("*"))
    .build();

  @Override
  public User getUser(String token) {
    return admin;
  }

  @Override
  public User getUser(UUID userId) {
    return admin.getId().equals(userId) ? admin : null;
  }

  @Override
  public String generateToken(User user, boolean expiring) {
    return "";
  }
}
