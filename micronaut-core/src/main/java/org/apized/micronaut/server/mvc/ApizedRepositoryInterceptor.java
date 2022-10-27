package org.apized.micronaut.server.mvc;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.data.model.Page;
import jakarta.inject.Singleton;
import org.apized.core.model.BaseModel;
import org.apized.core.model.Model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
@InterceptorBean(ApizedRepository.class)
public class ApizedRepositoryInterceptor implements MethodInterceptor<Object, Object> {
  private final Map<Class<?>, Class<?>> entities = new HashMap<>();

  public void addProxyMapping(Class<?> entity, Class<?> proxy) {
    entities.put(entity, proxy);
  }

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    Object result = context.proceed();
    Class<?> type = context.getReturnType().asArgument().getFirstTypeVariable().orElse(context.getReturnType().asArgument()).getType();
    if (result != null && entities.containsKey(type)) {
      BeanIntrospection<?> introspection = BeanIntrospection.getIntrospection(entities.get(type));

      if (Page.class.isAssignableFrom(result.getClass())) {
        Page<?> page = (Page<?>) result;
        result = Page.of(
          page.getContent().stream().map(it ->
            instantiate(introspection, it)
          ).toList(),
          page.getPageable(),
          page.getTotalSize()
        );
      } else if (Optional.class.isAssignableFrom(result.getClass()) && ((Optional<?>) result).isPresent()) {
        result = Optional.of(
          instantiate(introspection, ((Optional<?>) result).orElse(null))
        );
      } else if (Collection.class.isAssignableFrom(result.getClass())) {
        result = ((Collection<?>) result).stream().map(e ->
          instantiate(introspection, e)
        ).toList();
      } else {
        result = instantiate(introspection, result);
      }
    }
    return result;
  }

  private BaseModel instantiate(BeanIntrospection<?> introspection, Object obj) {
    BaseModel model = (BaseModel) introspection.instantiate(obj);
    model._getModelMetadata().setOriginal((Model) introspection.instantiate(obj));
    return model;
  }
}
