package org.apized.core.security.enricher;

import org.apized.core.context.ApizedContext;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.Model;
import org.apized.core.tracing.Traced;

import java.util.UUID;

public interface PermissionEnricherHandler<T extends Model> {
  @Traced(
    attributes = {
      @Traced.Attribute(key = "permission.action", arg = "action"),
      @Traced.Attribute(key = "permission.type", arg = "type")
    }
  )
  default boolean enrich(Class<Model> type, Action action, Execution<T> execution) {
    int before = ApizedContext.getSecurity().getUser().getPermissions().size();
    switch (action) {
      case CREATE -> create(execution, execution.getInput());
      case LIST -> list(execution);
      case GET -> get(execution, execution.getId());
      case UPDATE -> update(execution, execution.getId(), execution.getInput());
      case DELETE -> delete(execution, execution.getId());
    }
    return ApizedContext.getSecurity().getUser().getPermissions().size() > before;
  }

  default void list(Execution<T> execution) {
  }

  default void get(Execution<T> execution, UUID id) {
  }

  default void create(Execution<T> execution, T input) {
  }

  default void update(Execution<T> execution, UUID id, T input) {
  }

  default void delete(Execution<T> execution, UUID id) {
  }
}
