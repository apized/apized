package org.apized.micronaut.server.mvc;

import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class ProxyRegistry {
  private final Map<Class<?>, Class<?>> entities = new HashMap<>();

  public void addProxyMapping(Class<?> entity, Class<?> proxy) {
    entities.put(entity, proxy);
  }

  public boolean contains(Class<?> type) {
    return entities.containsKey(type);
  }

  public Class<?> get(Class<?> type) {
    return entities.get(type);
  }
}
