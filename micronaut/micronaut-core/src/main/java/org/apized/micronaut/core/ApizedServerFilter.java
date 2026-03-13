package org.apized.micronaut.core;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Inject;
import org.apized.core.ApizedConfig;

import java.util.List;
import java.util.regex.Pattern;

public abstract class ApizedServerFilter implements Ordered {

  @Inject
  protected ApizedConfig config;

  public @Nullable boolean shouldExclude(String uri) {
    if (CollectionUtils.isEmpty(config.getExclusions())) {
      return false;
    } else {
      List<Pattern> patterns = config.getExclusions().stream().map(Pattern::compile).toList();
      return patterns.stream().anyMatch((pattern) -> pattern.matcher(uri).matches());
    }
  }
}
