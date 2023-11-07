package org.apized.core.federation;

import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

public interface ModelClient<T extends Model> {
  default Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort) throws URISyntaxException, IOException, InterruptedException {
    return list(page, pageSize, search, sort, List.of());
  }

  Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort, List<String> fields) throws URISyntaxException, IOException, InterruptedException;

  default T get(UUID id) throws URISyntaxException, IOException, InterruptedException {
    return get(id, List.of());
  }

  T get(UUID id, List<String> fields) throws URISyntaxException, IOException, InterruptedException;

  default T create(T t) throws URISyntaxException, IOException, InterruptedException {
    return create(t, List.of());
  }

  T create(T t, List<String> fields) throws URISyntaxException, IOException, InterruptedException;

  default T update(T t) throws URISyntaxException, IOException, InterruptedException {
    return update(t, List.of());
  }

  T update(T t, List<String> fields) throws URISyntaxException, IOException, InterruptedException;

  default T delete(UUID id) throws URISyntaxException, IOException, InterruptedException {
    return delete(id, List.of());
  }

  T delete(UUID id, List<String> fields) throws URISyntaxException, IOException, InterruptedException;
}
