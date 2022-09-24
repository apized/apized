package org.apized.micronaut.server;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.DefaultArgument;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import org.apized.core.StringHelper;
import org.apized.core.model.BaseModel;
import org.apized.core.mvc.ModelService;
import org.apized.core.search.SearchHelper;
import org.apized.core.search.SearchOperation;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;

import java.util.*;
import java.util.stream.Collectors;

public class ModelResolver {
  public static ApplicationContext applicationContext;


  public static Object getModelValue(Class<? extends BaseModel> clazz, String field, UUID selfId) {
    return getModelValue(clazz, field, selfId, null, Map.of(), Map.of());
  }

  public static Object getModelValue(Class<? extends BaseModel> clazz, String field, UUID selfId, UUID otherId) {
    return getModelValue(clazz, field, selfId, otherId, Map.of(), Map.of());
  }

  @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
  public static Object getModelValue(Class<? extends BaseModel> clazz, String field, UUID selfId, UUID otherId, Map<String, Object> search, Map<String, Object> sort) {
    List<SearchTerm> terms = ((Map<String, Object>) search.getOrDefault(field, new HashMap<String, Object>())).keySet().stream().map(SearchHelper::convertTerm).filter(Objects::nonNull).collect(Collectors.toList());
    List<SortTerm> subSort = ((Map<String, Object>) sort.getOrDefault(field, new HashMap<String, Object>())).keySet().stream().map(SearchHelper::convertSort).filter(Objects::nonNull).collect(Collectors.toList());
    BeanProperty<?, Object> property = BeanIntrospection.getIntrospection(clazz).getProperty(field).orElse(null);

    Argument<Object> argument = Collection.class.isAssignableFrom(property.getType()) ? Argument.of(property.asArgument().getTypeParameters()[0].getType()) : property.asArgument();
    ModelService<?> service = applicationContext.getBean(new DefaultArgument<>(ModelService.class, null, argument));

    if (Collection.class.isAssignableFrom(property.getType())) {
      Optional<AnnotationValue<ManyToMany>> manyToMany = property.getAnnotationMetadata().findAnnotation(ManyToMany.class);
      Optional<AnnotationValue<OneToMany>> oneToMany = property.getAnnotationMetadata().findAnnotation(OneToMany.class);
      if (oneToMany.isPresent()) {
        Optional<String> mappedBy = oneToMany.flatMap(annotation -> annotation.stringValue("mappedBy"));
        terms.add(new SearchTerm(mappedBy.isEmpty() ? field : mappedBy.get(), SearchOperation.eq, selfId));
      } else if (manyToMany.isPresent()) {
        terms.add(new SearchTerm(field + ".id", SearchOperation.eq, selfId));
      } else {
        terms.add(new SearchTerm(StringHelper.uncapitalize(StringHelper.pluralize(property.getDeclaringType().getSimpleName())), SearchOperation.eq, List.of(selfId)));
      }
      return service.list(terms, subSort).getContent();
    } else if (otherId != null) {
      return service.get(otherId);
    } else {
      return null;
    }
  }
}
