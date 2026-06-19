package com.blikeng.chess.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into HTTP responses for all controllers.
 *
 * <p>An {@link ApiException} becomes a response with its own status and message (404s
 * logged at info, everything else at warn). Any other exception is logged at error and
 * returned as a generic 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleException(ApiException ex) {
        if (ex.getStatus() == HttpStatus.NOT_FOUND) {
            logger.info("API exception: {} {}", ex.getStatus(), ex.getMessage());
        } else {
            logger.warn("API exception: {} {}", ex.getStatus(), ex.getMessage());
        }

        return ResponseEntity
                .status(ex.getStatus())
                .body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        logger.error("Unexpected exception occurred: ", ex);
        return ResponseEntity
                .status(500)
                .body("Unexpected error occurred. Please try again later.");
    }
}
