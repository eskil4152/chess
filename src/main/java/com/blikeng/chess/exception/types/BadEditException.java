package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import org.springframework.http.HttpStatus;

public class BadEditException extends ApiException {
    public BadEditException() {
        super(HttpStatus.BAD_REQUEST, "Bad profile edit");
    }
}
