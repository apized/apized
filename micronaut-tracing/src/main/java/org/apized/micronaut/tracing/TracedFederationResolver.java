package org.apized.micronaut.tracing;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.inject.Singleton;
import org.apized.core.ApizedConfig;
import org.apized.core.model.Model;
import org.apized.core.mvc.AbstractModelService;
import org.apized.micronaut.federation.FederationResolver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Requires(bean = Tracer.class)
@Replaces(FederationResolver.class)
@Singleton
public class TracedFederationResolver extends FederationResolver {
  private Tracer tracer;

  public TracedFederationResolver(ApizedConfig config, ObjectMapper mapper, List<AbstractModelService<? extends Model>> services, Tracer tracer) {
    super(config, mapper, services);
    this.tracer = tracer;
  }

  @Override
  protected Map<String, Object> performRequest(URI url) throws Exception {
    Span span = tracer
      .spanBuilder(url.toString())
      .setSpanKind(SpanKind.CLIENT)
      .setAttribute("http.method", "GET")
      .setAttribute("http.url", url.toString())
      .startSpan();

    try (Scope ignore = span.makeCurrent()) {
      return super.performRequest(url);
    } catch (Throwable t) {
      span.setStatus(StatusCode.ERROR);
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      t.printStackTrace(pw);
      span.recordException(
        t,
        Attributes.of(
          AttributeKey.booleanKey("exception.escaped"), true,
          AttributeKey.stringKey("exception.message"), t.getMessage(),
          AttributeKey.stringKey("exception.stacktrace"), sw.toString(),
          AttributeKey.stringKey("exception.type"), t.getClass().getName()
        )
      );
      throw t;
    } finally {
      span.end();
    }
  }
}
