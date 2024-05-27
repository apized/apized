package org.apized.micronaut.tracing;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Singleton;
import org.apized.core.ApizedConfig;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;
import org.apized.micronaut.federation.FederationResolver;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Requires(bean = Tracer.class)
@Replaces(FederationResolver.class)
@Singleton
public class TracedFederationResolver extends FederationResolver {

  public TracedFederationResolver(ApizedConfig config, ObjectMapper mapper, List<AbstractModelService<? extends Model>> services) {
    super(config, mapper, services);
  }

  @Override
  @Traced(
    kind = SpanKind.CLIENT,
    attributes = {
      @Traced.Attribute(key = "http.method", value = "GET"),
      @Traced.Attribute(key = "http.url", arg = "url")
    }
  )
  protected Map<String, Object> performRequest(URI url) throws Exception {
    return super.performRequest(url);
  }
}
