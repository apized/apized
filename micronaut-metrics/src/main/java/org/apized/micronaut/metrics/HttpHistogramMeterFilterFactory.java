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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
   * @param minMs the minimum time (in ms) value expected to use in the histogram. Defaults to 100ms
   * @param maxMs the maximum (in ms) value expected to use in the histogram. Defaults to 60_000ms, i.e. 60s
   * @return A MeterFilter
   */
  @Bean
  @Singleton
  @Requires(property = MICRONAUT_METRICS_BINDERS + ".web.server.histogram")
  MeterFilter addServerPercentileMeterFilter(
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.enabled:false}") Boolean histogram,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.minMs:100}") int minMs,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.server.histogram.maxMs:60000}") int maxMs
  ) {
    return getMeterFilter(histogram, minMs, maxMs, WebMetricsPublisher.METRIC_HTTP_SERVER_REQUESTS);
  }

  /**
   * Configure new MeterFilter for http.client.requests metrics.
   *
   * @param histogram If a histogram should be published
   * @param minMs the minimum time (in ms) value expected to use in the histogram. Defaults to 100ms
   * @param maxMs the maximum (in ms) value expected to use in the histogram. Defaults to 60_000ms, i.e. 60s
   * @return A MeterFilter
   */
  @Bean
  @Singleton
  @Requires(property = MICRONAUT_METRICS_BINDERS + ".web.client.histogram")
  MeterFilter addClientPercentileMeterFilter(
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram:false}") Boolean histogram,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram.minMs:100}") int minMs,
    @Value("${" + MICRONAUT_METRICS_BINDERS + ".web.client.histogram.maxMs:60000}") int maxMs
  ) {
    return getMeterFilter(histogram, minMs, maxMs, WebMetricsPublisher.METRIC_HTTP_CLIENT_REQUESTS);
  }

  private MeterFilter getMeterFilter(Boolean histogram, int minMs, int maxMs, String metricNamePrefix) {
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().startsWith(metricNamePrefix)) {
          var min = (int) Math.floor(Math.log10(minMs)) + 1;
          var max = (int) Math.floor(Math.log10(maxMs)) + 1;

          List<Double> reduce = new ArrayList<>();
          IntStream.rangeClosed(min, max)
            .forEach(p ->
              IntStream.rangeClosed(0, 9)
                .forEach(i ->
                  IntStream.rangeClosed(0, 9)
                    .forEach(j -> {
                      double val = (Math.pow(10, p + 1) * i + (Math.pow(10, p) * j));
                      if (val <= maxMs * 10) {
                        reduce.add(val * 100_000d);
                      }
                    })
                )
            );

          return DistributionStatisticConfig.builder()
            .percentilesHistogram(histogram)
            .serviceLevelObjectives(
              reduce.stream().filter(i -> i > 0).mapToDouble(i -> i).toArray()
            )
            .minimumExpectedValue(minMs * 1_000_000d)
            .maximumExpectedValue(maxMs * 1_000_000d)
            .build()
            .merge(config);
        }
        return config;
      }
    };
  }
}
