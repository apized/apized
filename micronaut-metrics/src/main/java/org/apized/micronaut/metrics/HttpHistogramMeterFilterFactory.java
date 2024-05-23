/**
 * todo Remove this class since it should be temporary. PR submitted to upstream project.
 * https://github.com/micronaut-projects/micronaut-micrometer/pull/740
 */
package org.apized.micronaut.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.configuration.metrics.binder.web.WebMetricsPublisher;
import io.micronaut.context.annotation.*;
import jakarta.inject.Singleton;

import java.util.Arrays;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;
import static io.micronaut.core.util.StringUtils.FALSE;

/**
 * Optional filter for adding percentile to HTTP metrics.
 *
 * @author lcavadas
 */
@Factory
@RequiresMetrics
@Requires(property = WebMetricsPublisher.ENABLED, notEquals = FALSE)
@Primary
public class HttpHistogramMeterFilterFactory {

  /**
   * Configure new MeterFilter for http.server.requests metrics.
   *
   * @param histogram If a histogram should be published
   * @param min       the minimum time (in ms) value expected.
   * @param max       the maximum time (in ms) value expected.
   * @param slas      the user-defined service levels (in ms) to create.
   * @return A MeterFilter
   */
  @Bean
  @Singleton
  @Requires(property = MICRONAUT_METRICS_BINDERS + ".web.server.histogram")
  MeterFilter addServerPercentileMeterFilter(
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.enabled:false}") Boolean histogram,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.min:-1}") Double min,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.max:-1}") Double max,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.slas:}") Double[] slas
  ) {
    return getMeterFilter(histogram, min, max, slas, WebMetricsPublisher.METRIC_HTTP_SERVER_REQUESTS);
  }

  /**
   * Configure new MeterFilter for http.client.requests metrics.
   *
   * @param histogram If a histogram should be published
   * @param min       the minimum time (in ms) value expected.
   * @param max       the maximum time (in ms) value expected.
   * @param slas      the user-defined service levels (in ms) to create.
   * @return A MeterFilter
   */
  @Bean
  @Singleton
  @Requires(property = MICRONAUT_METRICS_BINDERS + ".web.client.histogram")
  MeterFilter addClientPercentileMeterFilter(
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram:false}") Boolean histogram,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram.min:-1}") Double min,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram.max:-1}") Double max,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram.slas:}") Double[] slas
  ) {
    return getMeterFilter(histogram, min, max, slas, WebMetricsPublisher.METRIC_HTTP_CLIENT_REQUESTS);
  }

  private MeterFilter getMeterFilter(Boolean histogram, Double minMs, Double maxMs, Double[] slas, String metricNamePrefix) {
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().startsWith(metricNamePrefix)) {
          var build = DistributionStatisticConfig.builder().percentilesHistogram(histogram);

          if (slas != null) {
            build.serviceLevelObjectives(
              Arrays.stream(slas).mapToDouble(v -> v * 1_000_000_000d).toArray()
            );
          }

          if (minMs != -1) {
            build.minimumExpectedValue(minMs * 1_000_000_000d);
          }

          if (maxMs != -1) {
            build.maximumExpectedValue(maxMs * 1_000_000_000d);
          }

          return build.build().merge(config);
        }
        return config;
      }
    };
  }
}
