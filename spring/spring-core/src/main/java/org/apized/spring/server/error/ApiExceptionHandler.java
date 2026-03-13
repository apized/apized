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

package org.apized.spring.server.error;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apized.core.error.exception.*;
import org.apized.core.error.model.ErrorEntry;
import org.apized.core.error.model.ErrorResponse;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleControllerException(HttpMediaTypeNotSupportedException exception) {
    log.info(exception.getLocalizedMessage(), exception);
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException exception) {
    log.info(exception.getMessage(), exception);
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleServiceUnavailableException(ServiceUnavailableException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.SERVICE_UNAVAILABLE
    );
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.FORBIDDEN
    );
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.UNAUTHORIZED
    );
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(JsonParseException.class)
  public ResponseEntity<ErrorResponse> handleJsonParseException(JsonParseException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MismatchedInputException.class)
  public ResponseEntity<ErrorResponse> handleMismatchedInputException(MismatchedInputException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.BAD_REQUEST
    );
  }

  /* Spring Exceptions */

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler({ServletRequestBindingException.class, UnsatisfiedServletRequestParameterException.class})
  public ResponseEntity<Void> handleApplicationException(Exception exception) {
    log.info(exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
  }

  @SneakyThrows
  @ExceptionHandler({TransactionSystemException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<?> handleTransactionSystemException(NestedRuntimeException exception) {
    Method candidateHandler = Arrays.stream(this.getClass().getMethods()).filter(method -> method.getParameters().length == 1 && method.getParameters()[0].getType().equals(exception.getRootCause().getClass())).findFirst().orElse(null);
    if (candidateHandler != null) {
      return (ResponseEntity<?>) candidateHandler.invoke(this, exception.getRootCause());
    } else {
      return handleException(exception.getRootCause());
    }
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
    log.info(exception.getMessage());
    ErrorResponse responses = new ErrorResponse();
    for (ObjectError error : exception.getBindingResult().getAllErrors()) {
      if (error instanceof FieldError) {
        responses.getErrors().add(
          ErrorEntry.builder()
            .entity(error.getObjectName())
            .field(((FieldError) error).getField())
            .message(error.getDefaultMessage())
            .build()
        );
      } else {
        responses.getErrors().add(
          ErrorEntry.builder()
            .entity(error.getObjectName())
            .message(error.getDefaultMessage())
            .build()
        );
      }
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responses);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
    log.info(exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
      ErrorResponse
        .builder()
        .message("Bad Request")
        .errors(
          exception.getConstraintViolations().stream().map(v ->
            ErrorEntry
              .builder()
              .entity(v.getLeafBean().getClass().getSimpleName().replaceAll("\\$Proxy$", ""))
              .field(
                StreamSupport.stream(v.getPropertyPath().spliterator(), false)
                  .map(node -> node.getName() + ((node.getIndex() != null) ? String.format("[%d]", node.getIndex()) : ""))
                  .collect(Collectors.joining("."))
              )
              .message(v.getMessage())
              .build()
          ).toList()
        )
        .build()
    );
  }

  @ExceptionHandler(InvalidFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFormatException(InvalidFormatException exception) {
    log.info(exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
      ErrorResponse.builder()
        .errors(List.of(ErrorEntry.builder()
          .entity(exception.getPath().get(0).getFrom().getClass().getSimpleName())
          .field(exception.getPath().get(0).getFieldName())
          .message(exception.getTargetType().isEnum() ? "must be " + Arrays.stream(exception.getTargetType().getEnumConstants()).map(Object::toString).toList() : exception.getOriginalMessage())
          .build()
        ))
        .build()
    );
  }

  @ExceptionHandler(value = DuplicateKeyException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityException(DuplicateKeyException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().message("Duplicate key found. Use PUT to update resource").build(),
      HttpStatus.CONFLICT
    );
  }

  @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.METHOD_NOT_ALLOWED
    );
  }

  @ExceptionHandler(value = DataAccessResourceFailureException.class)
  public ResponseEntity<Void> handleDBConnectionException(DataAccessResourceFailureException exception) {
    log.error(exception.getMessage(), exception);
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(DataAccessResourceFailureException exception) {
    log.info(exception.getMessage(), exception);
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.CONFLICT
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
    log.info(exception.getMessage(), exception);
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getRootCause().getMessage()).build())).build(),
      HttpStatus.CONFLICT
    );
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(NoResourceFoundException exception) {
    log.info(exception.getMessage());
    return new ResponseEntity<>(
      ErrorResponse.builder().errors(List.of(ErrorEntry.builder().message(exception.getMessage()).build())).build(),
      HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(value = Throwable.class)
  public ResponseEntity<ErrorResponse> handleException(Throwable exception) {
    log.error(exception.getMessage(), exception);
    return ResponseEntity.internalServerError().body(ErrorResponse
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
}
