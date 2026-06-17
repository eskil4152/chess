package com.blikeng.chess.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all expected API errors, carrying the {@link HttpStatus} and message that
 * {@link GlobalExceptionHandler} returns to the client.
 *
 * <p>Concrete errors are the subclasses in {@code exception.types}, each binding a fixed
 * status and an {@link ErrorMessages} constant (e.g. {@code GameNotFoundException} → 404).
 * Their names are self-documenting, so they aren't commented individually.
 */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}