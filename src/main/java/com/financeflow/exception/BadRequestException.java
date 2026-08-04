package com.financeflow.exception;

/**
 * Thrown when a request is well-formed but semantically invalid
 * (e.g. a date range whose start is after its end).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
