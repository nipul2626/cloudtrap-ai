
package com.cloudtrap.api.common.exception;

import com.cloudtrap.api.common.dto.ApiErrorResponse;
import com.cloudtrap.api.common.filter.RequestIdFilter;
import com.cloudtrap.api.identity.service.EmailAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationFailure(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldViolation> fieldErrors =
                Stream.concat(
                                exception.getBindingResult().getFieldErrors()
                                        .stream()
                                        .map(error ->
                                                new ApiErrorResponse.FieldViolation(
                                                        error.getField(),
                                                        safeValidationMessage(
                                                                error.getDefaultMessage()
                                                        )
                                                )),
                                exception.getBindingResult().getGlobalErrors()
                                        .stream()
                                        .map(error ->
                                                new ApiErrorResponse.FieldViolation(
                                                        "_request",
                                                        safeValidationMessage(
                                                                error.getDefaultMessage()
                                                        )
                                                ))
                        )
                        .distinct()
                        .sorted(Comparator
                                .comparing(
                                        ApiErrorResponse.FieldViolation::field
                                )
                                .thenComparing(
                                        ApiErrorResponse.FieldViolation::message
                                ))
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidationFailure(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request,
                List.of()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body is missing or malformed",
                request,
                List.of()
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "An account with this email address already exists",
                request,
                List.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        /*
         * The current registration service uses IllegalArgumentException
         * for invalid registration input. Never expose its message because
         * exception messages may contain sensitive or internal information.
         */
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "One or more request values are invalid",
                request,
                List.of()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "One or more request values are invalid",
                request,
                List.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        HttpHeaders headers = new HttpHeaders();

        if (exception.getSupportedHttpMethods() != null) {
            headers.setAllow(exception.getSupportedHttpMethods());
        }

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "The HTTP method is not supported for this endpoint",
                request,
                List.of(),
                headers
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        HttpHeaders headers = new HttpHeaders();

        if (exception.getSupportedMediaTypes() != null
                && !exception.getSupportedMediaTypes().isEmpty()) {
            headers.setAccept(exception.getSupportedMediaTypes());
        }

        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "The request content type is not supported",
                request,
                List.of(),
                headers
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_ACCEPTABLE,
                "NOT_ACCEPTABLE",
                "The requested response format is not available",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUEST_PARAMETER",
                "A required request parameter is missing",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "A request parameter has an invalid value or format",
                request,
                List.of()
        );
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestBindingFailure(
            ServletRequestBindingException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BINDING",
                "A required request value is missing or invalid",
                request,
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                request,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        String requestId = resolveRequestId(request);

        log.error(
                "Unhandled API exception: requestId={}, method={}, path={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldViolation> fieldErrors
    ) {
        return buildResponse(
                status,
                errorCode,
                message,
                request,
                fieldErrors,
                new HttpHeaders()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldViolation> fieldErrors,
            HttpHeaders additionalHeaders
    ) {
        String requestId = resolveRequestId(request);

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                request.getRequestURI(),
                requestId,
                fieldErrors
        );

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(additionalHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(RequestIdFilter.HEADER_NAME, requestId);

        return new ResponseEntity<>(body, headers, status);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(
                RequestIdFilter.REQUEST_ATTRIBUTE
        );

        if (attribute instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }

        /*
         * Defensive fallback for tests or dispatch paths that bypass
         * RequestIdFilter. Normal requests should use the filter's ID.
         */
        return UUID.randomUUID().toString();
    }

    private String safeValidationMessage(String message) {
        return message == null || message.isBlank()
                ? "Invalid value"
                : message;
    }
}
