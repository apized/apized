/*
 * Copyright 2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apized.micronaut.server.error;

import org.apized.core.error.ExceptionNotifier;
import org.apized.core.error.exception.ServerException;
import org.apized.micronaut.server.error.model.MicronautErrorEntry;
import org.apized.micronaut.server.error.model.MicronautErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class ApiExceptionHandler implements ExceptionHandler<ServerException, HttpResponse<MicronautErrorResponse>> {
  @Inject
  List<ExceptionNotifier> notifiers;

  @Override
  public HttpResponse<MicronautErrorResponse> handle(HttpRequest request, ServerException exception) {
    return switch (exception.getClass().getSimpleName()) {
      case "BadRequestException" -> HttpResponse.badRequest(MicronautErrorResponse.builder().errors(List.of(
        MicronautErrorEntry.builder().message(exception.getMessage()).build()
      )).build());
      case "ForbiddenException" ->
        HttpResponseFactory.INSTANCE.status(HttpStatus.FORBIDDEN).body(MicronautErrorResponse.builder().errors(List.of(
          MicronautErrorEntry.builder().message(exception.getMessage()).build()
        )).build());
      case "NotImplementedException", "NotFoundException" ->
        HttpResponse.notFound(MicronautErrorResponse.builder().errors(List.of(
          MicronautErrorEntry.builder().message(exception.getMessage()).build()
        )).build());
      case "ServiceUnavailableException" ->
        HttpResponseFactory.INSTANCE.status(HttpStatus.SERVICE_UNAVAILABLE).body(MicronautErrorResponse.builder().errors(List.of(
          MicronautErrorEntry.builder().message(exception.getMessage()).build()
        )).build());
      case "UnauthorizedException" -> HttpResponse.unauthorized();
      default -> {
        log.error(exception.getMessage(), exception);
        notifiers.forEach(n -> n.report(exception));
        yield HttpResponse.serverError(MicronautErrorResponse.builder().errors(List.of(
          MicronautErrorEntry.builder().message(String.format("Missing handler for [%s]: %s", exception.getClass(), exception.getMessage())).build()
        )).build());
      }
    };
  }
}
