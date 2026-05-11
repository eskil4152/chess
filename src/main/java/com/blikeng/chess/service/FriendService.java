package com.blikeng.chess.service;

import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.dto.FriendDTO;
import java.util.List;
import org.springframework.stereotype.Service;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.exception.types.InvalidUserException;

@Service
public class FriendService {
    private final FriendRepository friendRepository;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    public List<FriendDTO> getFriends() {
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        List<FriendEntity> friends = friendRepository.findFriendsForUser(principal.userId());

        return friends.stream()
                .map(friendship -> friendship.getUserA().getId().equals(principal.userId())
                ? friendship.getUserB() : friendship.getUserA())
                .map(friend -> new FriendDTO(
                        friend.getId(),
                        friend.getUsername(),
                        friend.getBio(),
                        friend.getAvatarUrl()
                )).toList();
    }
}
