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

package org.apized.spring.server.api.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apized.core.model.Apized;
import org.apized.core.model.BaseModel;
import org.apized.core.model.Layer;
import org.apized.spring.server.api.organization.Organization;

@Getter
@Setter
@Entity
@Apized(scope = Organization.class, layers = {Layer.SERVICE, Layer.REPOSITORY})
public class Address extends BaseModel {
  @NotNull
  @JsonIgnore
  @ManyToOne
  private Organization organization;

  @NotBlank
  private String line1;

  private String line2;

  @NotBlank
  private String city;

  @NotBlank
  private String postalCode;

  @NotBlank
  @Size(min = 2, max = 2)
  private String country;
}
