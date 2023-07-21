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

package org.apized.micronaut.server.api.department;

import org.apized.core.model.Apized;
import org.apized.core.model.ApiContext;
import org.apized.core.model.BaseModel;
import org.apized.micronaut.server.api.employee.Employee;
import org.apized.micronaut.server.api.organization.Organization;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Entity
@Serdeable
@Apized(scope = Organization.class)
public class Department extends BaseModel {
  @NotNull
  @JsonIgnore
  @ManyToOne
  private Organization organization;

  @NotNull
  private String name;

  @ManyToOne
  @ApiContext(property = "children")
  private Department parent;

  @OneToMany(mappedBy = "parent", orphanRemoval = true)
  private List<Department> children;

  @OneToMany(mappedBy = "department")
  private List<Employee> employees;
}
