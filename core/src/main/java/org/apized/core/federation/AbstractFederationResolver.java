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

package org.apized.core.federation;

import lombok.extern.slf4j.Slf4j;
import org.apized.core.ApizedConfig;
import org.apized.core.ModelMapper;
import org.apized.core.context.ApizedContext;
import org.apized.core.model.Apized;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
public abstract class AbstractFederationResolver {

  private final ApizedConfig config;

  private final List<AbstractModelService<? extends Model>> services;

  HttpClient client = HttpClient.newHttpClient();

  ModelMapper mapper;

  public AbstractFederationResolver(ApizedConfig config, List<AbstractModelService<? extends Model>> services) {
    this.config = config;
    this.services = services;

    this.mapper = new ModelMapper(Apized.class, Apized.class);
  }

  protected abstract Map<String, Object> parseResponse(String body) throws Exception;

  protected Map<String, Object> performRequest(URI url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(url)
      .header("AUTHORIZATION", String.format("Bearer %s", config.getToken()))
      .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return parseResponse(response.body());
  }

  public Object resolve(String service, String type, String uri, Object target, Set<String> fields) {

    if (target == null || (target instanceof Model && ((Model) target).getId() == null)) {
      return null;
    }

    if (fields.size() == 0) {
      return target;
    }

    if (service.equals(config.getSlug())) {
      log.info("Resolve {} (local) with id {}", type, target);
      return services
        .stream()
        .filter(it -> it.getType().getSimpleName().equals(type))
        .findFirst()
        .get()
        .get(((Model)target).getId());
    } else {
      try {
        URI url = createUri(
          config.getFederation().get(service) + uri + "?fields=" + String.join(",", fields),
          mapper.createMapOf(target)
        );

        if (ApizedContext.getFederation().getCache().containsKey(url)) {
          log.info("Resolve {} (cached) with id {} - {}", type, target, url);
          return ApizedContext.getFederation().getCache().get(url);
        } else {
          log.info("Resolve {} (remote) with id {} - {}", type, target, url);
          Map<String, Object> federated = performRequest(url);
          ApizedContext.getFederation().getCache().put(url, federated);
          return federated;
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return target;
      }
    }
  }

  protected URI createUri(String url, Map<String, Object> variables) throws URISyntaxException {
    String uri = url;
    for (String var : variables.keySet()) {
      uri = uri.replaceAll("\\{" + var + "}", variables.get(var).toString());
    }
    return new URI(uri);
  }
}
