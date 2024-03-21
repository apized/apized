package org.apized.micronaut.server.error;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

@Singleton
@SuppressWarnings("unchecked")
public class ErrorResponseHandler implements ErrorResponseProcessor<JsonError> {
  @Override
  public MutableHttpResponse<JsonError> processResponse(@NonNull ErrorContext errorContext, @NonNull MutableHttpResponse<?> response) {
    if (errorContext.getRequest().getMethod() == HttpMethod.HEAD) {
      return (MutableHttpResponse<JsonError>) response;
    }
    JsonError error;
    if (!errorContext.hasErrors()) {
      error = new JsonError(response.reason());
    } else {
      error = new JsonError(response.reason());
    }
    try {
      error.link(Link.SELF, Link.of(errorContext.getRequest().getUri()));
    } catch (IllegalArgumentException ignored) {
      // invalid URI, don't include it
    }

    return response.body(error).contentType(MediaType.APPLICATION_JSON_TYPE);
  }
}
