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

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import jakarta.persistence.*;
import lombok.*;
import org.apized.core.model.Action;
import org.apized.core.model.Model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_trail")
public class AuditEntry implements Model {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  private UUID transactionId;

  @Enumerated(EnumType.STRING)
  private Action action;

  private String type;

  @JsonIdentityReference(alwaysAsId = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID author;

  private String reason;

  private UUID target;

  //  @Type(type = "jsonb")
  @TypeDef(type = DataType.JSON)
  private Map<String, Object> payload;

  private LocalDateTime timestamp;

  @JsonIgnore
  private long epoch;
}
