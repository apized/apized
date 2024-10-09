package org.apized.spring.tracing;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MicrometerOpenTelemetryConfig {

  @Bean
  @Primary
  public OtlpMeterRegistry configureMetrics(
    @Value("${otel.sdk.disabled:false}") boolean disabled,
    @Value("${otel.exporter.otlp.endpoint:http://localhost:4318}") String url
  ) {
    return new OtlpMeterRegistry(
      new OtlpConfig() {
        @Override
        public String get(String key) {
          return null;
        }

        @Override
        public boolean enabled() {
          return !disabled;
        }

        @Override
        public String url() {
          return String.format("%s/v1/metrics", url);
        }
      },
      Clock.SYSTEM
    );
  }
}
