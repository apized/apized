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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apized.core.audit.annotation.AuditIgnore;
import org.apized.core.context.ApizedContext;
import org.apized.core.event.annotation.EventIgnore;
import org.apized.core.federation.Federation;

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
@SuperBuilder
public abstract class BaseModel implements Model {
  @Id
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID id;

  @AuditIgnore
  @EventIgnore
  @Version
  @Builder.Default
  private Long version = 0L;

  @AuditIgnore
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Federation(value = "auth", type = "User", uri = "/auth/users/{id}")
  private UUID createdBy;

  @AuditIgnore
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime createdAt;

  @AuditIgnore
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Federation(value = "auth", type = "User", uri = "/auth/users/{id}")
  private UUID lastUpdatedBy;

  @AuditIgnore
  @Temporal(TemporalType.TIMESTAMP)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime lastUpdatedAt;

  @Builder.Default
  @TypeDef(type = DataType.JSON)
  protected Map<String, Object> metadata = new HashMap<>();

  @JsonIgnore
  @Transient
  @Builder.Default
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
    createdBy = ApizedContext.getSecurity().getUser().getId();
    lastUpdatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    lastUpdatedBy = ApizedContext.getSecurity().getUser().getId();
    _getModelMetadata().setSaved(true);
  }

  @PreUpdate
  public void beforeUpdate() {
    lastUpdatedAt = LocalDateTime.now(ZoneId.of("UTC"));
    lastUpdatedBy = ApizedContext.getSecurity().getUser().getId();
    _getModelMetadata().setSaved(true);
  }
}
