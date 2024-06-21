package org.apized.micronaut.server.mvc;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.data.model.CursoredPage;
import io.micronaut.data.model.Page;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.model.BaseModel;
import org.apized.core.model.Model;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
@InterceptorBean(ApizedRepository.class)
public class ApizedRepositoryInterceptor implements MethodInterceptor<Object, Object> {

  @Inject
  ProxyRegistry proxyRegistry;

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    Object result = context.proceed();
    Class<?> type = context.getReturnType().asArgument().getFirstTypeVariable().orElse(context.getReturnType().asArgument()).getType();
    if (result != null && proxyRegistry.contains(type)) {
      BeanIntrospection<?> introspection = BeanIntrospection.getIntrospection(proxyRegistry.get(type));

      if (CursoredPage.class.isAssignableFrom(result.getClass())) {
        CursoredPage<?> page = (CursoredPage<?>) result;
        result = CursoredPage.of(
          page.getContent().stream().map(it ->
            instantiate(introspection, it)
          ).toList(),
          page.getPageable(),
          page.getCursors(),
          page.getTotalSize()
        );
      } else if (Page.class.isAssignableFrom(result.getClass())) {
        Page<?> page = (Page<?>) result;
        result = Page.of(
          page.getContent().stream().map(it ->
            instantiate(introspection, it)
          ).toList(),
          page.getPageable(),
          page.getTotalSize()
        );
      } else if (Optional.class.isAssignableFrom(result.getClass())) {
        result = Optional.ofNullable(
          instantiate(introspection, ((Optional<?>) result).orElse(null))
        );
      } else if (Collection.class.isAssignableFrom(result.getClass())) {
        result = ((Collection<?>) result).stream().map(e ->
          instantiate(introspection, e)
        ).toList();
      } else if (Stream.class.isAssignableFrom(result.getClass())) {
        result = ((Stream) result).map(it -> instantiate(introspection, it));
      } else {
        result = instantiate(introspection, result);
      }
    }
    return result;
  }

  private BaseModel instantiate(BeanIntrospection<?> introspection, Object obj) {
    BaseModel model = null;
    if (obj != null) {
      model = (BaseModel) introspection.instantiate(obj);
      model._getModelMetadata().setOriginal((Model) introspection.instantiate(obj));
    }
    return model;
  }
}
