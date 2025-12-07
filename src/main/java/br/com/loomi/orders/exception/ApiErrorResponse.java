package br.com.loomi.orders.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response structure.
 * Used to return consistent error information to clients.
 */
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details;

    /**
     * Gets the error timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the error timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param status the status code to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error code.
     *
     * @param error the error code to set
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message.
     *
     * @param message the error message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the request path where the error occurred.
     *
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the request path where the error occurred.
     *
     * @param path the request path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets additional error details.
     *
     * @return the error details list
     */
    public List<String> getDetails() {
        return details;
    }

    /**
     * Sets additional error details.
     *
     * @param details the error details list to set
     */
    public void setDetails(List<String> details) {
        this.details = details;
    }
}
