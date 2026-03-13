package org.apized.spring.server;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.DefaultArgument;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.apized.core.StringHelper;
import org.apized.core.model.BaseModel;
import org.apized.core.mvc.ModelService;
import org.apized.core.search.SearchHelper;
import org.apized.core.search.SearchOperation;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortTerm;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

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
    ModelService<?> service = (ModelService<?>) applicationContext.getBean(applicationContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(ModelService.class, argument.getType()))[0]);

    if (Collection.class.isAssignableFrom(property.getType())) {
      Optional<AnnotationValue<ManyToMany>> manyToMany = property.getAnnotationMetadata().findAnnotation(ManyToMany.class);
      Optional<AnnotationValue<OneToMany>> oneToMany = property.getAnnotationMetadata().findAnnotation(OneToMany.class);
      if (oneToMany.isPresent()) {
        Optional<String> mappedBy = oneToMany.flatMap(annotation -> annotation.stringValue("mappedBy"));
        terms.add(new SearchTerm(mappedBy.orElseGet(() -> StringHelper.uncapitalize(clazz.getSimpleName()).replaceAll("\\$Proxy$", "")), SearchOperation.eq, selfId));
      } else if (manyToMany.isPresent()) {
        BeanIntrospection<?> inverseModel = BeanIntrospection.getIntrospection(property.asArgument().getFirstTypeVariable().get().getType());
        Optional<? extends BeanProperty<?, Object>> inverseField = inverseModel
          .getBeanProperties()
          .stream()
          .filter(p ->
            p.getAnnotationMetadata().hasAnnotation(ManyToMany.class)
          )
          .filter(p ->
            p.asArgument().getTypeParameters()[0].getType().equals(property.getDeclaringType()) && (
              Objects.requireNonNull(p.getAnnotation(ManyToMany.class)).stringValue("mappedBy").orElse("").equals(field) ||
                p.getName().equals(manyToMany.get().stringValue("mappedBy").orElse(""))
            )
          )
          .findFirst();
        service = applicationContext.getBean(ModelService.class, null, new DefaultArgument<>(inverseModel.getBeanType(), inverseModel.getAnnotationMetadata()));
        terms.add(new SearchTerm(inverseField.get().getName() + ".id", SearchOperation.eq, selfId));
      } else {
        terms.add(new SearchTerm(StringHelper.uncapitalize(StringHelper.pluralize(property.getDeclaringType().getSimpleName())), SearchOperation.eq, List.of(selfId)));
      }
      return service.list(terms, subSort, true).getContent();
    } else if (otherId != null) {
      return service.find(otherId);
    } else if (selfId != null) {
      Optional<AnnotationValue<OneToOne>> oneToOne = property.getAnnotationMetadata().findAnnotation(OneToOne.class);
      if (oneToOne.isEmpty() || oneToOne.flatMap(annotation -> annotation.stringValue("mappedBy")).isEmpty()) {
        return null;
      }
      String mappedBy = oneToOne.flatMap(annotation -> annotation.stringValue("mappedBy")).get();
      return service.searchOne(new SearchTerm(mappedBy, SearchOperation.eq, selfId)).orElse(null);
    } else {
      return null;
    }
  }
}
