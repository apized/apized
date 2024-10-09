package org.apized.micronaut.server.serde;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.runtime.mapper.sql.SqlJsonValueMapper;
import io.micronaut.jackson.databind.JacksonDatabindMapper;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;

@Singleton
public class JacksonSqlJsonValueMapper implements SqlJsonValueMapper {

  private final JacksonDatabindMapper mapper;

  public JacksonSqlJsonValueMapper() {
    this.mapper = new JacksonDatabindMapper();
  }

  @Override
  public @NonNull JsonMapper getJsonMapper() {
    return mapper;
  }
}
