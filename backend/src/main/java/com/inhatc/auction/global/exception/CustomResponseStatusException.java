package com.inhatc.auction.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

public class CustomResponseStatusException extends ResponseStatusException {
    private final Map<String, String> errors;

    public CustomResponseStatusException(HttpStatus status, Map<String, String> errors) {
        super(status, "Validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

}