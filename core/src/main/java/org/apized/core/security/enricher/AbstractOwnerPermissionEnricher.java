package org.apized.core.security.enricher;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.core.type.Argument;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.core.error.exception.NotFoundException;
import org.apized.core.execution.Execution;
import org.apized.core.model.Action;
import org.apized.core.model.DummyModel;
import org.apized.core.model.Model;
import org.apized.core.mvc.ModelRepository;
import org.apized.core.security.annotation.Owner;
import org.apized.core.security.annotation.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractOwnerPermissionEnricher implements PermissionEnricherHandler<Model> {
  protected String slug;
  Logger log = LoggerFactory.getLogger(this.getClass());

  public AbstractOwnerPermissionEnricher(String slug, PermissionEnricherManager manager) {
    this.slug = slug;
    manager.registerEnricher(Model.class, this);
  }

  @Override
  public boolean enrich(Class<Model> type, Action action, Execution<Model> execution) {
    if (BeanIntrospection.getIntrospection(type).getBeanProperties().stream().anyMatch(prop -> prop.hasAnnotation(Owner.class))) {
      Optional<BeanProperty<Model, Object>> ownerProp = BeanIntrospection.getIntrospection(type).getBeanProperties().stream()
        .filter(property -> property.hasAnnotation(Owner.class))
        .findFirst();

      if (ownerProp.isPresent()) {
        List<String> inferredPermissions = new ArrayList<>();
        String entityName = StringHelper.uncapitalize(type.getSimpleName());
        AnnotationValue<Owner> annotation = ownerProp.get().getAnnotation(Owner.class);

        switch (action) {
          case LIST, CREATE -> {
            if (annotation.get("actions", Argument.of(List.class, Action.class)).get().contains(action)) {
              inferredPermissions.add(slug + "." + entityName + "." + action.getType());
            } else if (!annotation.get("permissions", Argument.of(List.class, Permission.class)).orElse(new ArrayList<>()).isEmpty()) {
              annotation.getAnnotations("permissions", Permission.class).forEach(p ->
                Arrays.stream(p.stringValues("fields")).forEach(field ->
                  inferredPermissions.add(slug + "." + entityName + "." + action.getType() + "." + field)
                )
              );
            }
          }
          case GET, UPDATE, DELETE -> {
            if (annotation.get("actions", Argument.of(List.class, Action.class)).get().contains(action) && currentUserIsOwner(type, execution, ownerProp.get())) {
              inferredPermissions.add(slug + "." + entityName + "." + action.getType() + "." + execution.getId());
            } else if (!annotation.get("permissions", Argument.of(List.class, Permission.class)).orElse(new ArrayList<>()).isEmpty()) {
              annotation.getAnnotations("permissions", Permission.class).forEach(p ->
                Arrays.stream(p.stringValues("fields")).forEach(field ->
                  inferredPermissions.add(slug + "." + entityName + "." + action.getType() + "." + execution.getId() + "." + field)
                )
              );
            }
          }
          default -> log.error("Unsupported action: {}", action);
        }

        if (!inferredPermissions.isEmpty()) {
          ApizedContext.getSecurity().getUser().getInferredPermissions().addAll(inferredPermissions);
          return true;
        }
      }
    }
    return false;
  }

  private boolean currentUserIsOwner(Class<Model> type, Execution<Model> execution, BeanProperty<Model, Object> prop) {
    UUID ownerId = null;
    Optional<Model> model =
      Optional.ofNullable(
        Optional.ofNullable(execution.getInput())
          .orElse(
            findBean(Argument.of(ModelRepository.class, type))
              .flatMap(r -> ((ModelRepository<? extends Model>) r).get(execution.getId()))
              .orElseThrow(NotFoundException::new)
          )
      );
    if (model.isEmpty()) return false;
    BeanWrapper<Model> wrapper = BeanWrapper.getWrapper(model.get());

    if (UUID.class.isAssignableFrom(prop.getType())) {
      ownerId = wrapper.getProperty(prop.getName(), UUID.class).orElse(null);
    } else if (Model.class.isAssignableFrom(prop.getType())) {
      ownerId = wrapper.getProperty(prop.getName(), Model.class).orElse(new DummyModel()).getId();
    }

    return ApizedContext.getSecurity().getUser().getId().equals(ownerId);
  }

  public abstract <K> Optional<K> findBean(Argument<K> argument);
}
