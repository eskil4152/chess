package com.blikeng.chess.exception.errorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class ExistingGameException extends ApiException {
    public ExistingGameException() {
        super(HttpStatus.CONFLICT, ErrorMessages.ALREADY_QUEUED);
    }
}
