package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class InvalidUUIDException extends ApiException {
    public InvalidUUIDException() {
        super(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_UUID);
    }
}
