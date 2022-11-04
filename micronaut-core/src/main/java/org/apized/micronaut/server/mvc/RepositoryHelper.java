/*
 * Copyright 2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apized.micronaut.server.mvc;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.data.model.DataType;
import io.micronaut.data.model.Sort;
import io.micronaut.data.model.jpa.criteria.PersistentEntityFrom;
import io.micronaut.data.model.runtime.RuntimePersistentEntity;
import io.micronaut.data.repository.jpa.criteria.QuerySpecification;
import io.micronaut.data.runtime.criteria.RuntimeCriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apized.core.ApizedConfig;
import org.apized.core.StringHelper;
import org.apized.core.context.ApizedContext;
import org.apized.core.error.exception.NotImplementedException;
import org.apized.core.model.Apized;
import org.apized.core.model.Model;
import org.apized.core.search.SearchOperation;
import org.apized.core.search.SearchTerm;
import org.apized.core.search.SortDirection;
import org.apized.core.search.SortTerm;
import org.apized.core.security.annotation.Owner;
import org.apized.micronaut.server.ModelResolver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class RepositoryHelper {
  public static <T extends Model> QuerySpecification<T> getQuerySpecification(List<SearchTerm> search) {
    return getQuerySpecification(search, false);
  }

  public static <T extends Model> QuerySpecification<T> getQuerySpecification(List<SearchTerm> search, boolean skipAutoFilters) {
    QuerySpecification<T> spec = (root, query, builder) -> {
      List<Predicate> criteria = new ArrayList<>();

      BeanIntrospection<Model> introspection = ((RuntimePersistentEntity) ((PersistentEntityFrom<?, ?>) root).getPersistentEntity()).getIntrospection();
      AnnotationValue<Apized> apized = introspection.getAnnotation(Apized.class);
      Map<String, UUID> pathVariables = ApizedContext.getRequest().getPathVariables();

      if (!skipAutoFilters) {
        //todo this probably needs to be recursive
        String slug = ApizedConfig.getInstance().getSlug();
        Arrays.stream(apized.classValues("scope")).forEach(s -> {
          String uncapitalize = StringHelper.uncapitalize(s.getSimpleName());
          if (pathVariables.get(uncapitalize) != null || !ApizedContext.getSecurity().getUser().isAllowed(slug + "." + uncapitalize + ".get")) {
            search.add(new SearchTerm(uncapitalize, SearchOperation.eq, pathVariables.get(uncapitalize)));

            BeanIntrospection.getIntrospection(s).getBeanProperties().stream()
              .filter(p -> p.getAnnotation(Owner.class) != null)
              .forEach(p -> {
                if (!ApizedContext.getSecurity().getUser().isAllowed(slug + "." + StringHelper.uncapitalize(introspection.getBeanType().getSimpleName()) + ".get")) {
                  search.add(new SearchTerm(uncapitalize + "." + p.getName(), SearchOperation.eq, ApizedContext.getSecurity().getUser().getId()));
                }
              });
          }
        });

        introspection.getBeanProperties()
          .stream()
          .filter(p -> p.getAnnotation(Owner.class) != null)
          .forEach(p -> {
            if (!ApizedContext.getSecurity().getUser().isAllowed(slug + "." + StringHelper.uncapitalize(introspection.getBeanType().getSimpleName()) + ".get")) {
              search.add(new SearchTerm(p.getName(), SearchOperation.eq, ApizedContext.getSecurity().getUser().getId()));
            }
          });
      }

      for (SearchTerm searchTerm : search) {
        String field = searchTerm.getField();
        Object value = searchTerm.getValue();
        From from = root;

        if (field.contains(".")) {
          List<String> split = List.of(searchTerm.getField().split("\\."));
          Optional<BeanProperty<Model, Object>> property = introspection.getProperty(split.get(0));

          if (property.isEmpty()) {
            continue;
          }

          from = ((PersistentEntityFrom<?, ?>) root).join(split.get(0), Join.Type.INNER);
          field = split.get(1);
        } else if (introspection.getProperty(field).get().getAnnotationMetadata().hasAnnotation(TypeDef.class)) {
          BeanProperty<Model, Object> property = introspection.getProperty(field).get();
          AnnotationValue<TypeDef> typeDef = property.getAnnotation(TypeDef.class);
          if (Objects.requireNonNull(typeDef).enumValue("type", DataType.class).orElse(DataType.STRING).equals(DataType.JSON)) {
            switch (ApizedConfig.getInstance().getDialect()) {
              case ANSI -> ansiMetadataQuery(root, builder, criteria, value, from, List.of(field), null);
              case H2 -> h2MetadataQuery(root, builder, criteria, value, from, List.of(field), null);
              case MYSQL -> mysqlMetadataQuery(root, builder, criteria, value, from, List.of(field), null);
              case ORACLE -> oracleMetadataQuery(root, builder, criteria, value, from, List.of(field), null);
              case POSTGRES -> postgresMetadataQuery(root, builder, criteria, value, from, List.of(field), null);
              case SQL_SERVER -> sqlServerMetadataQuery(root, builder, criteria, value, from, List.of(field), null);
            }
          }
          continue;
        }

        switch (searchTerm.getOp()) {
          case eq -> {
            if (value == null) {
              criteria.add(builder.isNull(from.get(field)));
            } else {
              criteria.add(builder.equal(from.get(field), value));
            }
          }
          case ne -> {
            if (value == null) {
              criteria.add(builder.isNotNull(from.get(field)));
            } else {
              criteria.add(builder.notEqual(from.get(field), value));
            }
          }
          case like -> criteria.add(((RuntimeCriteriaBuilder) builder).ilikeString(
            from.get(field),
            builder.literal("%" + value.toString() + "%"))
          );
          case gt -> criteria.add(builder.greaterThan(from.get(field), (Comparable) value));
          case gte -> criteria.add(builder.greaterThanOrEqualTo(from.get(field), (Comparable) value));
          case lt -> criteria.add(builder.lessThan(from.get(field), (Comparable) value));
          case lte -> criteria.add(builder.lessThanOrEqualTo(from.get(field), (Comparable) value));
        }
      }
      return builder.and(criteria.toArray(new Predicate[0]));
    };
    return spec;
  }

  private static <T extends Model> void ansiMetadataQuery(Root<T> root, CriteriaBuilder builder, List<Predicate> criteria, Object value, From from, List<String> split, BeanProperty<Model, Object> property) {
    throw new NotImplementedException();
  }

  private static <T extends Model> void h2MetadataQuery(Root<T> root, CriteriaBuilder builder, List<Predicate> criteria, Object value, From from, List<String> split, BeanProperty<Model, Object> property) {
    throw new NotImplementedException();
  }

  private static <T extends Model> void mysqlMetadataQuery(Root<T> root, CriteriaBuilder builder, List<Predicate> criteria, Object value, From from, List<String> split, BeanProperty<Model, Object> property) {
    throw new NotImplementedException();
  }

  private static <T extends Model> void oracleMetadataQuery(Root<T> root, CriteriaBuilder builder, List<Predicate> criteria, Object value, From from, List<String> split, BeanProperty<Model, Object> property) {
    throw new NotImplementedException();
  }

  private static <T extends Model> void postgresMetadataQuery(Root<T> root, CriteriaBuilder builder, List<Predicate> criteria, Object value, From from, List<String> split, BeanProperty<Model, Object> property) {
    boolean valid = false;
    CriteriaBuilder.In in = builder.in(from.get("id"));
    String field = split.get(0);

    try {
      ResultSet resultSet = ModelResolver.applicationContext.findBean(JdbcOperations.class).get().prepareStatement(
        "select id from " + StringHelper.uncapitalize(root.getJavaType().getSimpleName()) + " where " + field + " @> '" + value + "'",
        statement -> statement.executeQuery()
      );
      while (resultSet.next()) {
        valid = true;
        in.value(resultSet.getObject("id", UUID.class));
      }
    } catch (SQLException e) {

    }
    if (!valid) {
      //ensure we have at least one element otherwise the resulting sql expression will be invalid `in ()`
      in.value(UUID.randomUUID());
    }
    criteria.add(in);
  }

  private static <T extends Model> void sqlServerMetadataQuery(Root<T> root, CriteriaBuilder builder, List<Predicate> criteria, Object value, From from, List<String> split, BeanProperty<Model, Object> property) {
    throw new NotImplementedException();
  }

  public static <T extends Model> Sort generateSort(List<SortTerm> sort) {
    if (sort.size() == 0) {
      sort = List.of(new SortTerm("createdAt", SortDirection.desc));
    }

    return Sort.of(
      sort.stream().map(s ->
          s.getDirection() == SortDirection.asc ? Sort.Order.asc(s.getField()) : Sort.Order.desc(s.getField())
        )
        .toList()
        .toArray(new Sort.Order[0]));
  }
}
