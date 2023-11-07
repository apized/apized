package org.apized.micronaut.federation;

import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.apized.core.federation.ModelClient;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

public class MicronautModelClient<T extends Model> implements ModelClient<T> {
  private final HttpClient client;
  private final String baseUrl;

  private final Class<T> type;

  @Inject
  ObjectMapper mapper;

  @Inject
  ApizedConfig config;

  public MicronautModelClient(String modelBaseUrl, Class<T> type) {
    this.type = type;
    client = HttpClient.newHttpClient();
    baseUrl = modelBaseUrl;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort, List<String> fields) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .method("GET", HttpRequest.BodyPublishers.noBody())
      .uri(new URI(String.format("%s?fields=%s", baseUrl, String.join(",", fields))))
      .header("AUTHORIZATION", String.format("Bearer %s", config.getToken()))
      .build();

    return (Page<T>) mapper.readValue(
      client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      ).body(),
      Argument.of(Page.class, type)
    );
  }

  @Override
  public T get(UUID id, List<String> fields) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .method("GET", HttpRequest.BodyPublishers.noBody())
      .uri(new URI(String.format("%s/%s?fields=%s", baseUrl, id.toString(), String.join(",", fields))))
      .header("AUTHORIZATION", String.format("Bearer %s", config.getToken()))
      .build();

    return mapper.readValue(
      client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      ).body(),
      type
    );
  }

  @Override
  public T create(T t, List<String> fields) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .method("POST", HttpRequest.BodyPublishers.ofString(
        mapper.writeValueAsString(t)
      ))
      .uri(new URI(String.format("%s?fields=%s", baseUrl, String.join(",", fields))))
      .header("Content-Type", "application/json")
      .header("AUTHORIZATION", String.format("Bearer %s", config.getToken()))
      .build();

    return mapper.readValue(
      client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      ).body(),
      type
    );
  }

  @Override
  public T update(T t, List<String> fields) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .method("POST", HttpRequest.BodyPublishers.ofString(
        mapper.writeValueAsString(t)
      ))
      .uri(new URI(String.format("%s/%s?fields=%s", baseUrl, t.getId().toString(), String.join(",", fields))))
      .header("Content-Type", "application/json")
      .header("AUTHORIZATION", String.format("Bearer %s", config.getToken()))
      .build();

    return mapper.readValue(
      client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      ).body(),
      type
    );
  }

  @Override
  public T delete(UUID id, List<String> fields) throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .method("DELETE", HttpRequest.BodyPublishers.noBody())
      .uri(new URI(String.format("%s/%s?fields=%s", baseUrl, id.toString(), String.join(",", fields))))
      .header("AUTHORIZATION", String.format("Bearer %s", config.getToken()))
      .build();

    return mapper.readValue(
      client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      ).body(),
      type
    );
  }
}
