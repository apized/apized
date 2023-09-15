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

package org.apized.core.security.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Introspected
public class User extends Permissionable {
  protected UUID id;
  protected String username;
  protected String name;
  protected Map<String, Object> metadata = new HashMap<>();
  protected List<Role> roles = new ArrayList<>();

  @JsonIgnore
  protected List<String> inferredPermissions = new ArrayList<>();

  @Builder
  public User(UUID id, String username, String name, List<Role> roles, List<String> permissions, List<String> inferredPermissions, Map<String, Object> metadata) {
    this.id = id;
    this.username = username;
    this.name = name;
    if (roles != null) {
      this.roles = roles;
    }
    if (permissions != null) {
      this.permissions = permissions;
    }
    if (inferredPermissions != null) {
      this.inferredPermissions = inferredPermissions;
    }
    if (metadata != null) {
      this.metadata = metadata;
    }
  }

  @Override
  public List<String> getPermissions() {
    ArrayList<String> allPerms = new ArrayList<>(super.getPermissions());
    allPerms.addAll(inferredPermissions);
    return allPerms;
  }

  @JsonIgnore
  public boolean isAllowed(String perm) {
    if (super.isAllowed(perm)) {
      return true;
    }

    for (Role role : getRoles()) {
      if (role.isAllowed(perm)) {
        return true;
      }
    }

    return false;
  }
}
