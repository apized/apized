package org.apized.spring.mcp;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apized.core.context.ApizedContext;
import org.apized.core.security.UserResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@ConditionalOnBean(UserResolver.class)
public class McpContextInitializer {

  @Inject
  UserResolver resolver;

  public void init() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      ApizedContext.destroy();
      HttpServletRequest request = attributes.getRequest();
      String authorization = request.getHeader("Authorization");
      if (authorization != null) {
        String token = authorization.replaceAll("Bearer (.*)", "$1");
        ApizedContext.getSecurity().setToken(token);
        ApizedContext.getSecurity().setUser(resolver.getUser(token));
      }
    }
  }
}
