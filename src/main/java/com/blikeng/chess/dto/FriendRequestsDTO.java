package com.blikeng.chess.dto;

import java.util.List;

public record FriendRequestsDTO (
    List<FriendPreview> friendPreviews
){
}
