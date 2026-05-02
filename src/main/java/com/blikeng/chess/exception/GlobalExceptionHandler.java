package com.blikeng.chess.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<String> handleException(ApiException ex) {
        logger.warn("API exception: {} {}", ex.getStatus(), ex.getMessage(), ex);

        return ResponseEntity
                .status(ex.getStatus())
                .body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<String> handleException(Exception ex) {
        logger.error("Unexpected exception occurred: ", ex);
        return ResponseEntity
                .status(500)
                .body("Unexpected error occurred. Please try again later.");
    }
}
