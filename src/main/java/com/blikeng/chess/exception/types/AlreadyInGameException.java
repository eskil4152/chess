package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class AlreadyInGameException extends ApiException {
    public AlreadyInGameException() {
        super(HttpStatus.CONFLICT, ErrorMessages.ALREADY_IN_GAME);
    }
}
