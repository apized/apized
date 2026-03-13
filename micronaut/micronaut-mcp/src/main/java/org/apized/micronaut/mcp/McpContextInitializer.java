package org.apized.micronaut.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apized.core.context.ApizedContext;
import org.apized.core.security.UserResolver;

@Singleton
@Requires(bean = UserResolver.class)
public class McpContextInitializer {

  @Inject
  UserResolver resolver;

  public void init() {
    ServerRequestContext.currentRequest().ifPresent(request -> {
      ApizedContext.destroy();
      String authorization = request.getHeaders().get("Authorization");
      if (authorization != null) {
        String token = authorization.replaceAll("Bearer (.*)", "$1");
        ApizedContext.getSecurity().setToken(token);
        ApizedContext.getSecurity().setUser(resolver.getUser(token));
      }
    });
  }
}
