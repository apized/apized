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

import org.apized.core.model.Apized;
import org.apized.core.model.BaseModel;
import org.apized.micronaut.server.api.department.Department;
import org.apized.micronaut.server.api.employee.Employee;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Serdeable
@Apized(extensions = OrganizationRepositoryExtension.class)
public class Organization extends BaseModel {
  @NotBlank
  private String name;

  @NotNull
  @OneToOne(mappedBy = "organization")
  private Billing billing;

  @OneToMany(mappedBy = "organization", orphanRemoval = true)
  private List<Department> departments;

  @OneToMany(mappedBy = "organization", orphanRemoval = true)
  private List<Employee> employees;
}
