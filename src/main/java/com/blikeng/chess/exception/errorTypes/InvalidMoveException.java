package com.blikeng.chess.exception.errorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class InvalidMoveException extends ApiException {
    public InvalidMoveException() {
        super(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_MOVE);
    }
}
