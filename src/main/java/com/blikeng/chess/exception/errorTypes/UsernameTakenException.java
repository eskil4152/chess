package com.blikeng.chess.exception.errorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class UsernameTakenException extends ApiException {
    public UsernameTakenException() {
        super(HttpStatus.CONFLICT, ErrorMessages.USERNAME_TAKEN);
    }
}
