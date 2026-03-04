package com.inhatc.auction.global.exception;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ResponseStatusException;

public class CustomResponseStatusException extends ResponseStatusException {
    private final Map<String, String> errors;

    public CustomResponseStatusException(@NonNull HttpStatus status, @NonNull Map<String, String> errors) {
        super(Objects.requireNonNull(status), "Validation failed");
        this.errors = Objects.requireNonNull(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

}
