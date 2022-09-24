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

package org.apized.micronaut.server.error.model;

import org.apized.core.error.model.ErrorEntry;
import org.apized.core.error.model.ErrorResponse;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.util.List;

@Serdeable
public class MicronautErrorResponse extends ErrorResponse {
  @Builder
  public MicronautErrorResponse(String message, List<? extends ErrorEntry> errors) {
    super(message, errors);
  }
}
