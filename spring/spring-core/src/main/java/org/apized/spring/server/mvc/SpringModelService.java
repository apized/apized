package org.apized.spring.server.mvc;

import io.micronaut.core.type.Argument;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

import java.util.Optional;

public abstract class SpringModelService<T extends Model> extends AbstractModelService<T> {
  protected ApplicationContext appContext;

  public SpringModelService(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public <K> Optional<K> findBean(Argument<K> argument) {
    return (Optional<K>) Optional.ofNullable(appContext.getBean(appContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(argument.getType(), argument.getFirstTypeVariable().get().getType()))[0]));
  }
}
