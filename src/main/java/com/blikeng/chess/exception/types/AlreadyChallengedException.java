package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class AlreadyChallengedException extends ApiException {
    public AlreadyChallengedException() {
        super(HttpStatus.CONFLICT, ErrorMessages.ALREADY_CHALLENGED);
    }
}
