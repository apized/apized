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

import org.apized.core.model.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class User extends Permissionable implements Model {
  protected UUID id;
  protected String username;
  protected String name;
  protected List<Role> roles = new ArrayList<>();

  @JsonIgnore
  protected List<String> inferredPermissions = new ArrayList<>();

  @Builder
  public User(UUID id, String username, String name, List<Role> roles, List<String> permissions, List<String> inferredPermissions) {
    this.id = id;
    this.username = username;
    this.name = name;
    this.roles = roles;
    if (permissions != null) {
      this.permissions = permissions;
    }
    if (inferredPermissions != null) {
      this.inferredPermissions = inferredPermissions;
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
