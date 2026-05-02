package com.blikeng.chess.exception.errorTypes;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class InvalidPromotionException extends ApiException {
    public InvalidPromotionException() {
        super(HttpStatus.BAD_REQUEST, ErrorMessages.INVALID_PROMOTION);
    }
}
