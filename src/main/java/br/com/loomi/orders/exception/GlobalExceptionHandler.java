package br.com.loomi.orders.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.List;

/**
 * Global exception handler for the application.
 * Handles various exception types and returns appropriate error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles business exceptions and returns appropriate error responses.
     *
     * @param ex the business exception
     * @param request the HTTP servlet request
     * @return response entity containing error details
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setTimestamp(Instant.now());
        error.setStatus(ex.getStatus().value());
        error.setError(ex.getCode());
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    /**
     * Handles validation exceptions from request body validation.
     *
     * @param ex the method argument not valid exception
     * @param req the HTTP servlet request
     * @return response entity containing validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .toList();

        ApiErrorResponse error = new ApiErrorResponse();
        error.setTimestamp(Instant.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("VALIDATION_ERROR");
        error.setMessage("Invalid request");
        error.setPath(req.getRequestURI());
        error.setDetails(details);

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles constraint violation exceptions.
     *
     * @param ex the constraint violation exception
     * @param req the HTTP servlet request
     * @return response entity containing constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setTimestamp(Instant.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("VALIDATION_ERROR");
        error.setMessage(ex.getMessage());
        error.setPath(req.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles all other uncaught exceptions.
     *
     * @param ex the generic exception
     * @param req the HTTP servlet request
     * @return response entity containing generic error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setTimestamp(Instant.now());
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setError("INTERNAL_ERROR");
        error.setMessage("Unexpected error");
        error.setPath(req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
