package org.apized.spring.security;

import io.micronaut.core.type.Argument;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.apized.core.security.UserResolver;
import org.apized.core.security.enricher.AbstractOwnerPermissionEnricher;
import org.apized.core.security.enricher.PermissionEnricherManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean(UserResolver.class)
public class OwnerPermissionEnricher extends AbstractOwnerPermissionEnricher {
  @Inject
  ApplicationContext appContext;

  public OwnerPermissionEnricher(ApizedConfig config, PermissionEnricherManager manager) {
    super(config.getSlug(), manager);
  }

  @Override
  public <K> Optional<K> findBean(Argument<K> argument) {
    return (Optional<K>) appContext.getBean(argument.getType());
  }
}
