package com.blikeng.chess.service;

import com.blikeng.chess.repository.FriendRepository;
import org.springframework.stereotype.Service;

@Service
public class FriendService {
    private final FriendRepository friendRepository;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }


}
