package org.apized.spring.core;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apized.core.model.Model;
import org.apized.spring.federation.FederationResolver;
import org.apized.spring.server.serde.ModelDeserializer;
import org.apized.spring.server.serde.ModelSerializer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  List<ModelDeserializer<? extends Model>> deserializers;
  FederationResolver resolver;

  public WebMvcConfig(List<ModelDeserializer<? extends Model>> deserializers, FederationResolver resolver) {
    this.deserializers = deserializers;
    this.resolver = resolver;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    (converters.stream().filter(it -> it instanceof MappingJackson2HttpMessageConverter).toList()).forEach(it -> {
      SimpleModule module = new SimpleModule("ApizedModel");
      module.addSerializer(new ModelSerializer(resolver));
      for (ModelDeserializer<? extends Model> deserializer : deserializers) {
        module.addDeserializer((Class) deserializer.getType(), deserializer);
      }
      ((MappingJackson2HttpMessageConverter) it).getObjectMapper().registerModule(module);
    });
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
      .addMapping("/**")
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "PUT", "POST", "PATCH", "DELETE", "OPTIONS")
      .allowCredentials(true);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    WebContentInterceptor cache = new WebContentInterceptor();
    cache.setCacheControl(CacheControl.noCache().cachePrivate());
    registry.addInterceptor(cache);
  }

  @Bean
  public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> frb = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
    frb.addUrlPatterns("/**");
    frb.setOrder(Integer.MIN_VALUE);
    return frb;
  }
}
