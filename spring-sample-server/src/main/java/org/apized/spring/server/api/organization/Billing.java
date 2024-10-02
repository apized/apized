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

package org.apized.spring.server.api.organization;

import org.apized.core.model.Apized;
import org.apized.core.model.BaseModel;
import org.apized.core.model.Layer;
import org.apized.spring.server.api.address.Address;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@ToString(callSuper = true)
@Apized(scope = Organization.class, layers = {Layer.SERVICE, Layer.REPOSITORY})
public class Billing extends BaseModel {
  @NotNull
  @OneToOne
  @JsonIgnore
  private Organization organization;

  @NotBlank
  private String companyName;

  @NotBlank
  private String vatNumber;

  @NotNull
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Address address;
}
