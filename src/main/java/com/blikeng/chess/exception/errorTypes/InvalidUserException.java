package com.blikeng.chess.exception.errorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class InvalidUserException extends ApiException {
    public InvalidUserException() {
        super(HttpStatus.UNAUTHORIZED, ErrorMessages.INVALID_USER);
    }
}
