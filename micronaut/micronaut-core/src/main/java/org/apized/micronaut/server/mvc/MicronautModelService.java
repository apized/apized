package org.apized.micronaut.server.mvc;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import org.apized.core.context.ApizedContext;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.mvc.AbstractModelService;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class MicronautModelService<T extends Model> extends AbstractModelService<T> {
  protected ApplicationContext appContext;

  public MicronautModelService(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public <K> Optional<K> findBean(Argument<K> argument) {
    return appContext.findBean(argument);
  }

  @Override
  public Optional<T> searchOne(List<SearchTerm> search) {
    String key = generateKey("searchOne", search, List.of(), true);
    if (ApizedContext.getSerde().getCache().containsKey(key)) {
      return Optional.of((T) ApizedContext.getSerde().getCache().get(key));
    } else {
      Optional<T> t = super.searchOne(search);
      t.ifPresent(value -> ApizedContext.getSerde().getCache().put(key, value));
      return t;
    }
  }

  @Override
  public Page<T> list(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    String key = generateKey("list", search, sort, skipAutoFilters);
    if (ApizedContext.getSerde().getCache().containsKey(key)) {
      return (Page<T>) ApizedContext.getSerde().getCache().get(key);
    } else {
      Page<T> list = super.list(search, sort, skipAutoFilters);
      ApizedContext.getSerde().getCache().put(key, list);
      return list;
    }
  }

  private String generateKey(String methodName, List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    StringBuilder keyBuilder = new StringBuilder(getType().getSimpleName()).append("|").append(methodName);
    search.forEach(s -> keyBuilder.append("|").append(s.getField()).append(":").append(s.getOp()).append(":").append(s.getValue()));
    sort.forEach(s -> keyBuilder.append("|").append(s.getField()).append(":").append(s.getDirection()));
    keyBuilder.append("|").append(skipAutoFilters);
    return keyBuilder.toString();
  }
}
