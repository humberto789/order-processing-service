package br.com.loomi.orders.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for business rule violations.
 * Contains HTTP status, error code, and descriptive message.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * Constructs a business exception with HTTP status, error code, and message.
     *
     * @param status the HTTP status to return
     * @param code the error code
     * @param message the error message
     */
    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * Gets the HTTP status associated with this exception.
     *
     * @return the HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getCode() {
        return code;
    }
}
