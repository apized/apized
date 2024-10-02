package org.apized.spring.core;

import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.regex.Pattern;

public abstract class ApizedServerFilter extends OncePerRequestFilter implements Ordered {

  @Inject
  protected ApizedConfig config;

  public boolean shouldExclude(String uri) {
    if (CollectionUtils.isEmpty(config.getExclusions())) {
      return false;
    } else {
      List<Pattern> patterns = config.getExclusions().stream().map(Pattern::compile).toList();
      return patterns.stream().anyMatch((pattern) -> pattern.matcher(uri).matches());
    }
  }
}
