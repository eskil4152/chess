package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class BadEditException extends ApiException {
    public BadEditException() {
        super(HttpStatus.BAD_REQUEST, ErrorMessages.BAD_PROFILE_EDIT);
    }
}
