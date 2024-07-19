package org.apized.core.mvc;

import io.micronaut.context.annotation.Secondary;
import org.apized.core.error.exception.NotImplementedException;
import org.apized.core.model.Model;
import org.apized.core.model.Page;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Secondary
public class AbstractModelRepository<T extends Model> implements ModelRepository<T> {
  @Override
  public void add(String field, UUID self, UUID other) {
    throw new NotImplementedException();
  }

  @Override
  public void addMany(String field, List<ManyToManyTuple> adds) {
    throw new NotImplementedException();
  }

  @Override
  public void remove(String field, UUID self, UUID other) {
    throw new NotImplementedException();
  }

  @Override
  public void removeMany(String field, List<ManyToManyTuple> removes) {
    throw new NotImplementedException();
  }

  @Override
  public Page<T> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort) {
    throw new NotImplementedException();
  }

  @Override
  public Optional<T> searchOne(List<SearchTerm> search) {
    throw new NotImplementedException();
  }

  @Override
  public Page<T> search(List<SearchTerm> search, List<SortTerm> sort, boolean skipAutoFilters) {
    throw new NotImplementedException();
  }

  @Override
  public Optional<T> get(UUID id) {
    throw new NotImplementedException();
  }

  @Override
  public T create(T it) {
    throw new NotImplementedException();
  }

  @Override
  public List<T> batchCreate(List<T> it) {
    throw new NotImplementedException();
  }

  @Override
  public T update(UUID id, T it) {
    return it;
  }

  @Override
  public List<T> batchUpdate(List<T> it) {
    throw new NotImplementedException();
  }

  @Override
  public void delete(UUID id) {
    throw new NotImplementedException();
  }

  @Override
  public void batchDelete(List<UUID> it) {
    throw new NotImplementedException();
  }
}
