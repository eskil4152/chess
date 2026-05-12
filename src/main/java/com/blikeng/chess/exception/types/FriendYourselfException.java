package com.blikeng.chess.exception.types;

import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.ErrorMessages;
import org.springframework.http.HttpStatus;

public class FriendYourselfException extends ApiException {
    public FriendYourselfException() {
        super(HttpStatus.BAD_REQUEST, ErrorMessages.FRIEND_YOURSELF);
    }
}
