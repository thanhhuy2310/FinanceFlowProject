package com.financeflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException.class )
    public ResponseEntity<String> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex
    ){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
    @ExceptionHandler(InvalidCredentialException.class )
    public ResponseEntity<String> handleInvalidCredential(
            InvalidCredentialException ice
    ){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((ice.getMessage()));
    }

    @ExceptionHandler(CsvImportException.class)
    public ResponseEntity<String> handleCsvImport(CsvImportException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

}
