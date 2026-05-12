package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorMessages.NOT_FOUND);
    }
}
