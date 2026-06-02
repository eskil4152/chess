package com.blikeng.chess.service;

import java.util.List;

import com.blikeng.chess.entity.FriendRequestEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.notifications.events.FriendRequestEvent;
import com.blikeng.chess.repository.FriendRequestRepository;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.blikeng.chess.dto.FriendDTO;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.FriendId;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final FriendRequestRepository friendRequestRepository;

    private final ApplicationEventPublisher eventPublisher;

    public FriendService(FriendRepository friendRepository, UserRepository userRepository, PresenceService presenceService, FriendRequestRepository friendRequestRepository, ApplicationEventPublisher eventPublisher) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
        this.friendRequestRepository = friendRequestRepository;
        this.eventPublisher = eventPublisher;
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
                    friend.getAvatarUrl(),
                    !presenceService.hasNoSessions(friend.getId())
                )).toList();
    }

    @Transactional
    public void sendFriendRequest(UsernameDTO usernameDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        if (principal.username().equals(usernameDTO.username())) throw new FriendYourselfException();

        UserEntity user = userRepository.findByUsernameIgnoreCase(principal.username()).orElseThrow(InvalidUserException::new);
        UserEntity friend = userRepository.findByUsernameIgnoreCase(usernameDTO.username()).orElseThrow(NotFoundException::new);

        if (friendRepository.existsById(FriendId.generate(user.getId(), friend.getId()))) {
            throw new AlreadyFriendsException();
        }

        if (friendRequestRepository.existsByFromUserAndToUser(user.getId(), friend.getId())) {
            throw new RequestExistsException();
        }

        if (friendRequestRepository.existsByFromUserAndToUser(friend.getId(), user.getId())) {
            // Accept friend request and return
        }

        FriendRequestEntity friendRequest = friendRequestRepository.save(new FriendRequestEntity(user.getId(), friend.getId()));

        eventPublisher.publishEvent(new FriendRequestEvent(friendRequest.getId(), user, friend));
    }

    public void removeFriend(UsernameDTO usernameDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        UserEntity user = userRepository.findByUsernameIgnoreCase(principal.username()).orElseThrow(InvalidUserException::new);
        UserEntity friend = userRepository.findByUsernameIgnoreCase(usernameDTO.username()).orElseThrow(NotFoundException::new);

        FriendId friendId = FriendId.generate(user.getId(), friend.getId());
        if (!friendRepository.existsById(friendId)) {
            throw new NotFoundException();
        }

        friendRepository.deleteById(friendId);
    }
}
