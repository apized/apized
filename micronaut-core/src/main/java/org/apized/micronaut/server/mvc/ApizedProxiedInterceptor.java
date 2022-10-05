package org.apized.micronaut.server.mvc;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.data.model.Page;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ApizedProxiedInterceptor implements MethodInterceptor<Object, Object> {
  private Map<Class<?>, Class<?>> entities = new HashMap<>();

  public void addProxyMapping(Class<?> entity, Class<?> proxy) {
    entities.put(entity, proxy);
  }

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    Object result = context.proceed();
    if (result != null) {
      if(Page.class.isAssignableFrom(result.getClass())){
        Page<?> page = (Page<?>) result;
        result = Page.of(
          page.getContent().stream().map(it ->
            BeanIntrospection.getIntrospection(
              entities.get(context.getReturnType().asArgument().getFirstTypeVariable().get().getType())
            ).instantiate(it)
          ).toList(),
          page.getPageable(),
          page.getTotalSize()
        );
      }else if (
        Optional.class.isAssignableFrom(result.getClass()) &&
        entities.containsKey(context.getReturnType().asArgument().getFirstTypeVariable().get().getType()) &&
        ((Optional<?>) result).isPresent()
      ) {
        result = Optional.of(
          BeanIntrospection.getIntrospection(
            entities.get(context.getReturnType().asArgument().getFirstTypeVariable().get().getType())
          ).instantiate(((Optional<?>) result).orElse(null))
        );
      } else if (
        Collection.class.isAssignableFrom(result.getClass()) &&
        entities.containsKey(context.getReturnType().asArgument().getFirstTypeVariable().get().getType())
      ) {
        result = ((Collection<?>) result).stream().map(e ->
          BeanIntrospection.getIntrospection(
            entities.get(context.getReturnType().asArgument().getFirstTypeVariable().get().getType())
          ).instantiate(e)
        ).toList();
      } else if (entities.containsKey(context.getReturnType().getType())) {
        result = BeanIntrospection.getIntrospection(entities.get(context.getReturnType().getType())).instantiate(result);
      }
    }
    return result;
  }
}
