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

import org.apized.core.federation.AbstractFederationResolver;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;
import org.apized.micronaut.core.ApizedConfig;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Singleton
public class FederationResolver extends AbstractFederationResolver {
  private final ApizedConfig config;
  @Inject
  HttpClient client;

  public FederationResolver(ApizedConfig config, List<AbstractModelService<? extends Model>> services) {
    super(
      config.getSlug(),
      config.getFederation(),
      services
    );
    this.config = config;
  }

  @Override
  protected Map<String, Object> performRequest(URI url) throws Exception {
    return (Map<String, Object>) client.toBlocking().retrieve(
      HttpRequest.<Map<String, Object>>GET(url)
        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", config.getToken())),
      Map.class
    );
  }
}
