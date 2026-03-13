package org.apized.micronaut.security;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.apized.core.security.UserResolver;
import org.apized.core.security.enricher.AbstractOwnerPermissionEnricher;
import org.apized.core.security.enricher.PermissionEnricherManager;

import java.util.Optional;

@Context
@Requires(bean = UserResolver.class)
public class OwnerPermissionEnricher extends AbstractOwnerPermissionEnricher {
  @Inject
  ApplicationContext appContext;

  public OwnerPermissionEnricher(ApizedConfig config, PermissionEnricherManager manager) {
    super(config.getSlug(), manager);
  }

  @Override
  public <K> Optional<K> findBean(Argument<K> argument) {
    return appContext.findBean(argument);
  }
}
