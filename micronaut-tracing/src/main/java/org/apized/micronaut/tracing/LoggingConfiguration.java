package org.apized.micronaut.tracing;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogLimits;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import jakarta.inject.Singleton;

@Singleton
@Requires(property = "micronaut.otel.enabled", value = "true")
public class LoggingConfiguration {
  @Value("${micronaut.application.name}")
  protected String name;

  @Value("${otel.exporter.otlp.endpoint}")
  private String endpoint;

  @Value("${otel.exporter.otlp.key}")
  private String key;

  @Value("${otel.exporter.otlp.instance}")
  private String instance;

  @EventListener
  void startupHandler(ServerStartupEvent event) {
    addLoggingExporter(name, endpoint, key, instance);
  }

  public static void addLoggingExporter(String name, String endpoint, String key, String instance) {

    var resource = Resource.getDefault().toBuilder()
      .put(ResourceAttributes.SERVICE_NAME, name.toLowerCase().replaceAll("\\s", "-"))
      .put(ResourceAttributes.SERVICE_INSTANCE_ID, instance)
      .build();

    var logExporter =
      OtlpGrpcLogRecordExporter.builder()
        .setEndpoint(endpoint)
        .setCompression("gzip")
        .addHeader("api-key", key)
        .build();

    SdkLoggerProvider logEmitterProvider =
      SdkLoggerProvider.builder()
        .setResource(resource)
        .setLogLimits(() -> LogLimits.getDefault().toBuilder().setMaxAttributeValueLength(4095).build())
        .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
        .build();

    OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setLoggerProvider(logEmitterProvider).build();

    GlobalLoggerProvider.set(logEmitterProvider);
  }
}
