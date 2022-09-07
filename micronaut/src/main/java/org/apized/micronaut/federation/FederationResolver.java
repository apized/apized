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

package org.apized.micronaut.federation;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.federation.AbstractFederationResolver;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;
import org.apized.micronaut.core.ApizedConfig;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

//todo eventually replace this with Services that forward the operations
// this should simplify a bit as well as expanding the federation to more than retrieve only.
@Singleton
public class FederationResolver extends AbstractFederationResolver {
  private final ApizedConfig config;
  @Inject
  HttpClient client;

  @Inject
  ObjectMapper mapper;

  public FederationResolver(ApizedConfig config, List<AbstractModelService<? extends Model>> services) {
    super(
      config.getSlug(),
      config.getFederation(),
      services
    );
    this.config = config;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Map<String, Object> performRequest(URI url) throws Exception {
    return client.toBlocking().retrieve(
      HttpRequest
        .<Map<String, Object>>GET(url)
        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", config.getToken())),
      Map.class
    );
  }
}
