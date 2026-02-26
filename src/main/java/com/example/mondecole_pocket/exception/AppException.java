package com.example.mondecole_pocket.exception;

public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AppException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AppException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
