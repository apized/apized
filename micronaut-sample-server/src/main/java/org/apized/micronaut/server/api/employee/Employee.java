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

package org.apized.micronaut.server.api.employee;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import org.apized.core.federation.Federation;
import org.apized.core.model.Apized;
import org.apized.core.model.BaseModel;
import org.apized.micronaut.server.api.address.Address;
import org.apized.micronaut.server.api.department.Department;
import org.apized.micronaut.server.api.organization.Organization;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Apized(scope = {Organization.class, Department.class}, extensions = EmployeeRepositoryExtension.class)
public class Employee extends BaseModel {
  @NotNull
  @JsonIgnore
  @ManyToOne
  private Organization organization;

  private String name;

  @NotNull
  @OneToOne(orphanRemoval = true)
  private Address address;

  private int age;

  private String position;

  private int salary;

  @ManyToOne
//  @ApiContext(property = "employees")
  private Department department;

  @TypeDef(type = DataType.JSON)
  @Federation(value = "catalogopolis", type = "Doctor", uri = "/objects/{catalogopolisId}")
  private Doctor favoriteDoctor;
}
