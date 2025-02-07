package com.inhatc.auction.global.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        HashMap<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        });

        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            errors.put(globalError.getObjectName(), globalError.getDefaultMessage());
        });

        log.error("Validation failed: {}", errors);

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(CustomResponseStatusException.class)
    public ResponseEntity<?> handleCustomResponseStatusException(CustomResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getErrors());
    }
}
