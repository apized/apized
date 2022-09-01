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

package org.apized.micronaut.security;

import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import jakarta.inject.Inject;
import org.apized.core.model.Model;
import org.apized.core.security.model.User;
import org.apized.micronaut.server.serde.ModelSerde;

import java.io.IOException;

public class UserDeserializer implements Deserializer<User> {
  @Inject
  ModelSerde serde;

  @Override
  public User deserialize(Decoder decoder, Deserializer.DecoderContext context, Argument<? super User> type) throws IOException {
    return (User) serde.deserialize(decoder, context, (Argument<? super Model>) type);
  }
}
