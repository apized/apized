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

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.error.ExceptionNotifier;
import org.apized.core.error.exception.ServerException;
import org.apized.core.error.model.ErrorEntry;
import org.apized.core.error.model.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Singleton
@Replaces(ConstraintExceptionHandler.class)
public class ApiExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<ErrorResponse>> {
  @Inject
  List<ExceptionNotifier> notifiers;

  List<ElementKind> excludedPathKinds = List.of(ElementKind.METHOD, ElementKind.PARAMETER, ElementKind.CONSTRUCTOR);

  @Override
  public HttpResponse<ErrorResponse> handle(HttpRequest request, Throwable exception) {
    if (exception instanceof ServerException) {
      log.info(exception.getMessage());
    } else {
      log.error(exception.getMessage(), exception);
    }
    return switch (exception.getClass().getSimpleName()) {
      case "ConstraintViolationException" -> HttpResponse.badRequest(
        ErrorResponse
          .builder()
          .message("Bad Request")
          .errors(
            ((ConstraintViolationException) exception).getConstraintViolations().stream().map(v ->
              ErrorEntry
                .builder()
                .entity(v.getLeafBean().getClass().getSimpleName().replaceAll("\\$Proxy$", ""))
                .field(
                  StreamSupport.stream(v.getPropertyPath().spliterator(), false)
                    .filter(node -> !excludedPathKinds.contains(node.getKind()))
                    .map(node -> node.getName() + ((node.getIndex() != null) ? String.format("[%d]", node.getIndex()) : ""))
                    .collect(Collectors.joining("."))
                )
                .message(v.getMessage())
                .build()
            ).toList()
          )
          .build()
      );
      case "BadRequestException" -> HttpResponse.badRequest(
        ErrorResponse
          .builder()
          .errors(
            List.of(
              ErrorEntry
                .builder()
                .message(exception.getMessage())
                .build()
            )
          )
          .build()
      );
      case "ForbiddenException" -> HttpResponseFactory.INSTANCE.status(HttpStatus.FORBIDDEN).body(
        ErrorResponse
          .builder()
          .errors(
            List.of(
              ErrorEntry
                .builder()
                .message(exception.getMessage())
                .build()
            )
          )
          .build()
      );
      case "NotImplementedException", "NotFoundException" -> HttpResponse.notFound(
        ErrorResponse
          .builder()
          .errors(
            List.of(
              ErrorEntry
                .builder()
                .message(exception.getMessage())
                .build()
            )
          )
          .build()
      );
      case "ServiceUnavailableException" -> HttpResponseFactory.INSTANCE.status(HttpStatus.SERVICE_UNAVAILABLE).body(
        ErrorResponse
          .builder()
          .errors(
            List.of(
              ErrorEntry
                .builder()
                .message(exception.getMessage())
                .build()
            )
          )
          .build()
      );
      case "UnauthorizedException" -> HttpResponse.unauthorized().body(ErrorResponse.builder().errors(
        List.of(
          ErrorEntry
            .builder()
            .message(exception.getMessage())
            .build()
        )
      ).build());
      case "IllegalArgumentException", "IllegalStateException" -> HttpResponse.badRequest().body(ErrorResponse.builder().errors(
        List.of(
          ErrorEntry
            .builder()
            .message(exception.getMessage())
            .build()
        )
      ).build());
      default -> {
        notifiers.forEach(n -> n.report(exception));
        yield HttpResponse.serverError(
          ErrorResponse
            .builder()
            .errors(
              List.of(
                ErrorEntry
                  .builder()
                  .message(
                    String.format(
                      "%s: %s",
                      exception.getClass().getSimpleName(),
                      exception.getMessage()
                    )
                  ).build()
              )
            )
            .build()
        );
      }
    };
  }
}
