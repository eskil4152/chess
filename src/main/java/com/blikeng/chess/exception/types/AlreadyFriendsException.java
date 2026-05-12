package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class AlreadyFriendsException extends ApiException {
    public AlreadyFriendsException() {
        super(HttpStatus.CONFLICT, ErrorMessages.ALREADY_FRIENDS);
    }
}
