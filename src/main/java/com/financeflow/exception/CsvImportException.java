package com.financeflow.exception;

/**
 * Thrown when an uploaded CSV file cannot be imported because of a structural
 * problem (empty file, unsupported extension, missing or invalid header).
 */
public class CsvImportException extends RuntimeException {

    public CsvImportException(String message) {
        super(message);
    }
}
