package org.apized.spring.tracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Tracer;
import org.apized.core.ApizedConfig;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;
import org.apized.core.tracing.TraceKind;
import org.apized.core.tracing.Traced;
import org.apized.spring.federation.FederationResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Primary
@Component
@ConditionalOnBean(Tracer.class)
public class TracedFederationResolver extends FederationResolver {

  public TracedFederationResolver(ApizedConfig config, ObjectMapper mapper, List<AbstractModelService<? extends Model>> services) {
    super(config, mapper, services);
  }

  @Override
  @Traced(
    kind = TraceKind.CLIENT,
    attributes = {
      @Traced.Attribute(key = "http.method", value = "GET"),
      @Traced.Attribute(key = "http.url", arg = "url")
    }
  )
  protected Map<String, Object> performRequest(URI url) throws Exception {
    return super.performRequest(url);
  }
}
