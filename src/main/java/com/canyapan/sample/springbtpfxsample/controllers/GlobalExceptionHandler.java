package com.canyapan.sample.springbtpfxsample.controllers;

import com.canyapan.sample.springbtpfxsample.exceptions.BaseException;
import com.canyapan.sample.springbtpfxsample.exceptions.InternalException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global exception handler for controller-layer exceptions.
 *
 * <p>This {@code @ControllerAdvice} centralizes translation of exceptions into
 * {@link org.springframework.http.ResponseEntity} responses containing a simple
 * {@code ErrorResponse} payload with a human-friendly reason and message.
 * Handlers choose an appropriate {@link org.springframework.http.HttpStatus} and
 * log the exception at a level appropriate for the error type.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    /**
     * Handles application-specific exceptions that extend {@link com.canyapan.sample.springbtpfxsample.exceptions.BaseException}.
     *
     * <p>If the thrown exception class is annotated with {@link ResponseStatus}, this status (and its reason)
     * are used for the response. Otherwise the handler falls back to the {@link com.canyapan.sample.springbtpfxsample.exceptions.InternalException}
     * {@code @ResponseStatus} to determine the status and reason.
     *
     * @param exception the application exception
     * @return a {@code ResponseEntity<ErrorResponse>} with the selected HTTP status and an error payload
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException exception) {
        ResponseStatus responseStatus = AnnotationUtils.getAnnotation(exception.getClass(), ResponseStatus.class);
        if (null == responseStatus) {
            responseStatus = AnnotationUtils.getAnnotation(InternalException.class, ResponseStatus.class);
        }

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(responseStatus.code())
                .body(new ErrorResponse(responseStatus.reason(), getExceptionMessage(exception)));

        if (response.getStatusCode().is4xxClientError()) {
            log.warn("A client-side error occurred,", exception);
        } else {
            log.error("A server-side error occurred,", exception);
        }
        return response;
    }

    /**
     * Handles {@link ConstraintViolationException} raised by bean validation on method parameters.
     *
     * <p>Builds a consolidated message that lists each violated constraint and responds with
     * HTTP 400 (Bad Request) and an {@code ErrorResponse} describing the violations.
     *
     * @param exception the constraint violation exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 400 and details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        final StringBuilder sb = new StringBuilder("Validation exception occurred.");
        final AtomicInteger counter = new AtomicInteger(1);
        exception.getConstraintViolations().forEach(e ->
                sb.append(" [%d], on property '%s', %s.".formatted(counter.getAndIncrement(), e.getPropertyPath(), e.getMessage())));

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Bad Request", sb.toString()));

        log.warn("A client-side validation error occurred. Constraint violation.", exception);

        return response;
    }

    /**
     * Handles missing request values such as headers, cookies, query parameters or path variables.
     *
     * <p>Maps the specific missing-value exception to a friendly message and returns
     * HTTP 400 (Bad Request) with an {@code ErrorResponse} describing which value is missing.
     *
     * @param exception the missing request value exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 400 and a descriptive message
     */
    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorResponse> handleValidationBadRequest(MissingRequestValueException exception) {
        String message;
        if (exception instanceof MissingRequestHeaderException) {
            message = String.format("A validation error occurred. Missing required header '%s'.", ((MissingRequestHeaderException) exception).getHeaderName());
        } else if (exception instanceof MissingRequestCookieException) {
            message = String.format("A validation error occurred. Missing required cookie '%s'.", ((MissingRequestCookieException) exception).getCookieName());
        } else if (exception instanceof MissingServletRequestParameterException) {
            message = String.format("A validation error occurred. Missing required query parameter '%s'.", ((MissingServletRequestParameterException) exception).getParameterName());
        } else if (exception instanceof MissingPathVariableException) {
            message = String.format("A validation error occurred. Missing required path parameter '%s'.", ((MissingPathVariableException) exception).getVariableName());
        } else {
            message = "A validation error occurred. Missing required value.";
        }

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Bad Request", message));

        log.warn("A client-side validation error occurred. Missing required value.", exception);

        return response;
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException} when a request parameter cannot be converted
     * to the expected target type (for example, non-numeric value for a numeric parameter).
     *
     * <p>Responds with HTTP 406 (Not Acceptable) and an {@code ErrorResponse} indicating the malformed field.
     *
     * @param exception the type mismatch exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 406 and details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleValidationNotAcceptable(MethodArgumentTypeMismatchException exception) {
        String message = String.format("A validation error occurred. Malformed value on field '%s'.", exception.getName());
        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.NOT_ACCEPTABLE)
                .body(new ErrorResponse("Not Acceptable", message));

        log.warn("A client-side validation error occurred. Not acceptable value.", exception);

        return response;
    }

    /**
     * Handles {@link HttpRequestMethodNotSupportedException} when the HTTP method is not supported by the endpoint.
     *
     * <p>Returns HTTP 405 (Method Not Allowed) and an {@code ErrorResponse} listing supported methods.
     *
     * @param exception the method-not-supported exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 405 and supported methods information
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        String message = String.format("Invalid http method received. It may be wrong or not supported by this operation. Try, %s.",
                Arrays.toString(exception.getSupportedMethods()));

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("Method Not Allowed", message));

        log.warn("A client-side error occurred. Method not allowed.", exception);
        return response;
    }

    /**
     * Handles {@link MethodArgumentNotValidException} produced by {@code @Valid} bean validation on request bodies.
     *
     * <p>Aggregates field errors into a single message and responds with HTTP 422 (Unprocessable Content)
     * and an {@code ErrorResponse} describing the invalid fields.
     *
     * @param exception the method-argument validation exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 422 and validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleVaLidationBadRequest(MethodArgumentNotValidException exception) {

        final StringBuilder sb = new StringBuilder("Validation exception occurred.");
        final AtomicInteger counter = new AtomicInteger(1);
        exception.getBindingResult().getAllErrors().forEach(e ->
                sb.append(" [%d], on field '%s', %s.".formatted(counter.getAndIncrement(), ((FieldError) e).getField(), e.getDefaultMessage())));

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErrorResponse("Unprocessable Content", sb.toString()));

        log.warn("A client-side validation error occurred. Unprocessable content.", exception);

        return response;
    }

    /**
     * Handles {@link HttpMessageNotReadableException} when the request body cannot be parsed (malformed JSON, bad date format, etc.).
     *
     * <p>Returns HTTP 400 (Bad Request) with an {@code ErrorResponse} explaining the parsing failure.
     *
     * @param exception the unreadable message exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 400 and a parsing error message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableInputBadRequest(HttpMessageNotReadableException exception) {

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Bad Request", "Cannot parse request body. Malformed input received."));

        log.warn("A client-side error occurred. Malformed input.", exception);

        return response;
    }

    /**
     * Handles Spring Security {@link AccessDeniedException} (authorization failures).
     *
     * <p>Responds with HTTP 403 (Forbidden) and an {@code ErrorResponse} indicating restricted access.
     *
     * @param exception the access denied exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedExceptions(AccessDeniedException exception) {

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Forbidden", "Forbidden access to content."));

        log.error("A client-side error occurred. Forbidden access.", exception);

        return response;
    }

    /**
     * Catches all other uncaught exceptions and maps them to HTTP 500 (Internal Server Error).
     *
     * <p>Returns a generic {@code ErrorResponse} and logs the exception as an error to aid
     * investigation while avoiding leaking internal details to clients.
     *
     * @param exception the unexpected exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 500 and a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnmanagedException(Exception exception) {

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal Server Error", "A server-side error occurred. Please reach out to the application support team if this issue persists."));

        log.error("A server-side error occurred. Unmanaged exception.", exception);

        return response;
    }

    /**
     * Handles {@link NoResourceFoundException} when requested resources are not found.
     *
     * @param exception the not-found exception
     * @return a {@code ResponseEntity<ErrorResponse>} with HTTP 404 and a short message
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException exception) {

        ResponseEntity<ErrorResponse> response = ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", "Requested content not found."));

        log.warn("A server-side error occurred. Not-Found exception.", exception);

        return response;
    }

    private String getExceptionMessage(final Exception e) {
        final String message = e.getMessage();

        return StringUtils.substringBefore(message, "; nested exception");
    }

    private record ErrorResponse(String reason, String message) {
    }

}
