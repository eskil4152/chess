package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class RequestExistsException extends ApiException {
    public RequestExistsException() {
        super(HttpStatus.CONFLICT, ErrorMessages.REQUEST_EXISTS);
    }
}
