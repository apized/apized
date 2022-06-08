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

package org.apized.core.audit.model;

import org.apized.core.model.Action;
import org.apized.core.model.Model;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_trail")
public class AuditEntry implements Model {

  @Id
  private UUID id = UUID.randomUUID();

  private UUID transactionId;

  @Enumerated(EnumType.STRING)
  private Action action;

  private String type;

  @JsonIdentityReference(alwaysAsId = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID by;

  private String reason;

  private UUID target;

  //  @Type(type = "jsonb")
  @TypeDef(type = DataType.JSON)
  private Map<String, Object> payload;

  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  @JsonIgnore
  private long epoch;

  public AuditEntry(UUID transactionId, Action action, String type, UUID by, String reason, UUID target, Map<String, Object> payload, Date timestamp, long epoch) {
    this.transactionId = transactionId;
    this.action = action;
    this.type = type;
    this.by = by;
    this.reason = reason;
    this.target = target;
    this.payload = payload;
    this.timestamp = timestamp;
    this.epoch = epoch;
  }
}
