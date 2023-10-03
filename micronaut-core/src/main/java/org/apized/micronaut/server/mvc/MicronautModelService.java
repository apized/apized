package org.apized.micronaut.server.mvc;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;

import java.util.Optional;

public abstract class MicronautModelService<T extends Model> extends AbstractModelService<T> {
  protected ApplicationContext appContext;

  public MicronautModelService(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public <K> Optional<K> findBean(Argument<K> argument) {
    return appContext.findBean(argument);
  }
}
