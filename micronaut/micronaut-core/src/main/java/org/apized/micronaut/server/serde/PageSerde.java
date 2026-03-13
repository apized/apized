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

package org.apized.micronaut.server.serde;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.Serde;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.context.ApizedContext;
import org.apized.core.model.Model;
import org.apized.core.model.Page;

import java.io.IOException;
import java.util.Map;

@Singleton
public class PageSerde implements Serde<Page<Object>> {
  @Inject
  ModelSerde serde;

  @Nullable
  @Override
  public Page<Object> deserialize(Decoder decoder, DecoderContext context, Argument<? super Page<Object>> type) throws IOException {
    return (Page<Object>) serde.deserialize(decoder, context, (Argument<Object>) type);
  }

  @Override
  public void serialize(Encoder encoder, EncoderContext context, Argument<? extends Page<Object>> type, Page<Object> value) throws IOException {
    if (!ApizedContext.getRequest().getFields().containsKey("content")) {
      ApizedContext.getRequest().setFields(Map.of("*", Map.of(), "content", ApizedContext.getRequest().getFields().isEmpty() ? Map.of("*", Map.of()) : ApizedContext.getRequest().getFields()));
      ApizedContext.getRequest().setSearch(Map.of("content", ApizedContext.getRequest().getSearch()));
      ApizedContext.getRequest().setSort(Map.of("content", ApizedContext.getRequest().getSort()));
    }
    serde.serialize(encoder, context, type, value);
  }
}
