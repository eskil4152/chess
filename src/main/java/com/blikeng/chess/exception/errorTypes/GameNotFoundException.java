package com.blikeng.chess.exception.errorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class GameNotFoundException extends ApiException {
    public GameNotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorMessages.GAME_NOT_FOUND);
    }
}
