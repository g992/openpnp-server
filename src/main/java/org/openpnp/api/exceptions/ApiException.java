package org.openpnp.api.exceptions;

/**
 * Базовое исключение для API OpenPnP
 */
public class ApiException extends Exception {
    private final int statusCode;
    private final String errorCode;

    public ApiException(String message) {
        this(500, "INTERNAL_ERROR", message);
    }

    public ApiException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}