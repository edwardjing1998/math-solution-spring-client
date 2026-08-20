package com.example.mathsolution.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(
            WebClientResponseException exception) {

        String responseBody = exception.getResponseBodyAsString();

        log.error(
                "Databricks request failed: status={}, responseBody={}",
                exception.getStatusCode(),
                responseBody,
                exception
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", exception.getStatusCode().value());
        body.put("error", "Databricks API request failed");
        body.put("message", responseBody.isBlank()
                ? exception.getMessage()
                : responseBody);

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception exception) {

        // The final "exception" argument prints the complete stack trace.
        log.error("Unexpected error while processing math solution request",
                exception);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", exception.getMessage() == null
                ? exception.getClass().getName()
                : exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}