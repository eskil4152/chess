package com.blikeng.chess.service;

import java.util.List;
import java.util.UUID;

import com.blikeng.chess.dto.*;
import com.blikeng.chess.entity.FriendRequestEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.notifications.events.FriendRequestEvent;
import com.blikeng.chess.repository.FriendRequestRepository;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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

        if (friendRequestRepository.existsByFromUser_IdAndToUser(user.getId(), friend.getId())) {
            throw new RequestExistsException();
        }

        if (friendRequestRepository.existsByFromUser_IdAndToUser(friend.getId(), user.getId())) {
            FriendId friendId = FriendId.generate(user.getId(), friend.getId());

            friendRepository.save(new FriendEntity(friendId, user, friend));
            friendRequestRepository.deleteByFromUser_IdAndToUser(friend.getId(), user.getId());

            return;
        }

        FriendRequestEntity friendRequest = friendRequestRepository.save(new FriendRequestEntity(user, friend.getId()));

        eventPublisher.publishEvent(new FriendRequestEvent(friendRequest.getId(), user, friend));
    }

    @Transactional
    public void respondToFriendRequest(FriendRequestResponseDTO friendRequestResponseDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        UUID requestId;

        try {
            requestId = UUID.fromString(friendRequestResponseDTO.id());
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDException();
        }

        FriendRequestEntity friendRequest = friendRequestRepository.findById(requestId).orElseThrow(NotFoundException::new);

        UserEntity receiver = userRepository.findById(friendRequest.getToUser()).orElseThrow(InvalidUserException::new);
        if (!receiver.getId().equals(principal.userId())) throw new NotFoundException();

        UserEntity sender = friendRequest.getFromUser();

        if (friendRequestResponseDTO.accepted()) {
            FriendId friendId = FriendId.generate(sender.getId(), receiver.getId());

            friendRepository.save(new FriendEntity(friendId, sender, receiver));
        }

        friendRequestRepository.deleteById(requestId);
    }

    public FriendRequestsDTO getFriendRequests() {
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        List<FriendPreview> previews = friendRequestRepository.findAllByToUser(principal.userId())
            .stream()
            .map(req -> new FriendPreview(req.getFromUser().getUsername(), req.getFromUser().getAvatarUrl()))
            .toList();

        return new FriendRequestsDTO(previews);
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
