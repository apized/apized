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

package org.apized.core.model;

import org.apized.core.federation.Federated;
import org.apized.core.federation.Federation;
import org.apized.core.security.SecurityContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
@ToString
@Introspected
public abstract class BaseModel implements Model {
  @Id
//  @GeneratedValue(strategy = GenerationType.AUTO)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID id;

  @Version
  private Long version = 0L;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @TypeDef(type = DataType.JSON)
  @Federation(value = "auth", type = "User", uri = "/auth/users/{id}")
  private Federated createdBy;

  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime createdAt;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @TypeDef(type = DataType.JSON)
  @Federation(value = "auth", type = "User", uri = "/auth/users/{id}")
  private Federated lastUpdatedBy;

  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime lastUpdatedAt;

  @TypeDef(type = DataType.JSON)
  protected Map<String, Object> metadata = new HashMap<>();

  @JsonIgnore
  @Transient
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  ModelMetadata _modelMetadata = new ModelMetadata();

  @Override
  public ModelMetadata _getModelMetadata() {
    return _modelMetadata;
  }

  @PrePersist
  public void beforeCreate() {
    id = id != null ? id : UUID.randomUUID();
    createdAt = LocalDateTime.now(ZoneId.of("UTC"));
    createdBy = new Federated(SecurityContext.getInstance().getUser().getId());
    lastUpdatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    lastUpdatedBy = new Federated(SecurityContext.getInstance().getUser().getId());
    _getModelMetadata().setSaved(true);
  }

  @PreUpdate
  public void beforeUpdate() {
    lastUpdatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    lastUpdatedBy = new Federated(SecurityContext.getInstance().getUser().getId());
    _getModelMetadata().setSaved(true);
  }
}
