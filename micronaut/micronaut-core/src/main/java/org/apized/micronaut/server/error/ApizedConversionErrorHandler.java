package org.apized.micronaut.server.error;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.server.exceptions.ConversionErrorHandler;
import io.micronaut.http.server.exceptions.ErrorExceptionHandler;
import io.micronaut.http.server.exceptions.response.Error;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Replaces(ConversionErrorHandler.class)
public class ApizedConversionErrorHandler  extends ErrorExceptionHandler<ConversionErrorException>  {
  @Inject ApiExceptionHandler handler;

  /**
   * Constructor.
   *
   * @param responseProcessor Error Response Processor
   */
  protected ApizedConversionErrorHandler(ErrorResponseProcessor<?> responseProcessor) {
    super(responseProcessor);
  }

  @Override
  protected @NonNull Error error(ConversionErrorException exception) {
    return null;
  }

  @Override
  public HttpResponse<?> handle(HttpRequest request, ConversionErrorException exception) {
    return handler.handle(request, exception);
  }
}
