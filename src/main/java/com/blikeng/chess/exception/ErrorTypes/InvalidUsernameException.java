package com.blikeng.chess.exception.ErrorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class InvalidUsernameException extends ApiException {
    public InvalidUsernameException() {
        super(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_USERNAME);
    }
}
