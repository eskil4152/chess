package com.blikeng.chess.unit.exception;

import com.blikeng.chess.exception.errorTypes.GameNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import com.blikeng.chess.exception.GlobalExceptionHandler;
import com.blikeng.chess.exception.ErrorMessages;

class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturnCorrectExceptionStatusAndMessage() {
        GameNotFoundException ex = new GameNotFoundException();
        ResponseEntity<String> response = handler.handleException(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(ErrorMessages.GAME_NOT_FOUND);
    }

    @Test
    void shouldHandleUnknownExceptionsAsInternalError() {
        Exception ex = new RuntimeException("boom");
        ResponseEntity<String> response = handler.handleException(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).contains("Unexpected error");
    }
}
