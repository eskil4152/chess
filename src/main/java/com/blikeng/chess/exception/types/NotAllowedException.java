package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class NotAllowedException extends ApiException {
    public NotAllowedException() {
        super(HttpStatus.FORBIDDEN, ErrorMessages.NOT_ALLOWED);
    }
}
