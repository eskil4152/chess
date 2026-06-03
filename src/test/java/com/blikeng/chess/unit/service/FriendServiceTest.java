package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.FriendDTO;
import com.blikeng.chess.dto.FriendRequestResponseDTO;
import com.blikeng.chess.dto.FriendRequestsDTO;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.FriendId;
import com.blikeng.chess.entity.FriendRequestEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.notifications.events.FriendRequestEvent;
import com.blikeng.chess.repository.FriendRequestRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.FriendService;
import com.blikeng.chess.service.PresenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock FriendRepository friendRepository;
    @Mock UserRepository userRepository;
    @Mock PresenceService presenceService;
    @Mock FriendRequestRepository friendRequestRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks FriendService friendService;

    private UserEntity currentUser;
    private UserEntity otherUser;

    @BeforeEach
    void setup() {
        currentUser = new UserEntity("me", "hash");
        otherUser = new UserEntity("alice", "hash");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext() {
        var principal = new JwtPrincipal(currentUser.getId(), currentUser.getUsername(), UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
    }

    private void setupSecurityContextWithNullUserId() {
        var principal = new JwtPrincipal(null, currentUser.getUsername(), UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
    }

    // --- Get Friends ---

    @Test
    void getFriendsShouldThrowWhenPrincipalIsNull() {
        assertThatThrownBy(() -> friendService.getFriends())
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void getFriendsShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();
        assertThatThrownBy(() -> friendService.getFriends())
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void getFriendsShouldReturnFriendWhenCurrentUserIsUserA() {
        setupSecurityContext();
        FriendId id = FriendId.generate(currentUser.getId(), otherUser.getId());
        FriendEntity friendship = new FriendEntity(id, currentUser, otherUser);
        when(friendRepository.findFriendsForUser(currentUser.getId())).thenReturn(List.of(friendship));
        when(presenceService.hasNoSessions(any())).thenReturn(true);

        List<FriendDTO> result = friendService.getFriends();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().username()).isEqualTo("alice");
    }

    @Test
    void getFriendsShouldReturnFriendWhenCurrentUserIsUserB() {
        setupSecurityContext();
        FriendId id = FriendId.generate(otherUser.getId(), currentUser.getId());
        FriendEntity friendship = new FriendEntity(id, otherUser, currentUser);
        when(friendRepository.findFriendsForUser(currentUser.getId())).thenReturn(List.of(friendship));
        when(presenceService.hasNoSessions(any())).thenReturn(true);

        List<FriendDTO> result = friendService.getFriends();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().username()).isEqualTo("alice");
    }

    @Test
    void getFriendsShouldReturnEmptyListWhenNoFriends() {
        setupSecurityContext();
        when(friendRepository.findFriendsForUser(currentUser.getId())).thenReturn(List.of());

        assertThat(friendService.getFriends()).isEmpty();
    }

    // --- Send Friend Request ---

    @Test
    void sendFriendRequestShouldThrowWhenPrincipalIsNull() {
        assertThatThrownBy(() -> friendService.sendFriendRequest(new UsernameDTO("alice")))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void sendFriendRequestShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();
        assertThatThrownBy(() -> friendService.sendFriendRequest(new UsernameDTO("alice")))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void sendFriendRequestShouldThrowWhenSendingToSelf() {
        setupSecurityContext();
        assertThatThrownBy(() -> friendService.sendFriendRequest(new UsernameDTO("me")))
                .isInstanceOf(FriendYourselfException.class);
        verify(friendRepository, never()).save(any());
    }

    @Test
    void sendFriendRequestShouldThrowWhenTargetNotFound() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.sendFriendRequest(new UsernameDTO("nobody")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void sendFriendRequestShouldThrowWhenAlreadyFriends() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(true);

        assertThatThrownBy(() -> friendService.sendFriendRequest(new UsernameDTO("alice")))
                .isInstanceOf(AlreadyFriendsException.class);
    }

    @Test
    void sendFriendRequestShouldThrowWhenRequestAlreadyExists() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(false);
        when(friendRequestRepository.existsByFromUser_IdAndToUser(currentUser.getId(), otherUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> friendService.sendFriendRequest(new UsernameDTO("alice")))
                .isInstanceOf(RequestExistsException.class);
    }

    @Test
    void sendFriendRequestShouldAutoAcceptWhenReceiverHasPendingRequest() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(false);
        when(friendRequestRepository.existsByFromUser_IdAndToUser(currentUser.getId(), otherUser.getId())).thenReturn(false);
        when(friendRequestRepository.existsByFromUser_IdAndToUser(otherUser.getId(), currentUser.getId())).thenReturn(true);

        friendService.sendFriendRequest(new UsernameDTO("alice"));

        verify(friendRepository).save(any(FriendEntity.class));
        verify(friendRequestRepository).deleteByFromUser_IdAndToUser(otherUser.getId(), currentUser.getId());
        verify(friendRequestRepository, never()).save(any(FriendRequestEntity.class));
    }

    @Test
    void sendFriendRequestShouldSaveRequestAndPublishEvent() {
        setupSecurityContext();
        FriendRequestEntity savedRequest = new FriendRequestEntity(currentUser, otherUser.getId());
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(false);
        when(friendRequestRepository.existsByFromUser_IdAndToUser(any(), any())).thenReturn(false);
        when(friendRequestRepository.save(any())).thenReturn(savedRequest);

        friendService.sendFriendRequest(new UsernameDTO("alice"));

        verify(friendRequestRepository).save(any(FriendRequestEntity.class));
        verify(eventPublisher).publishEvent(any(FriendRequestEvent.class));
    }

    // --- Respond To Friend Request ---

    @Test
    void respondToFriendRequestShouldThrowWhenPrincipalIsNull() {
        assertThatThrownBy(() -> friendService.respondToFriendRequest(
                new FriendRequestResponseDTO(UUID.randomUUID().toString(), true)))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void respondToFriendRequestShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();
        assertThatThrownBy(() -> friendService.respondToFriendRequest(
                new FriendRequestResponseDTO(UUID.randomUUID().toString(), true)))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void respondToFriendRequestShouldThrowOnInvalidUUID() {
        setupSecurityContext();
        assertThatThrownBy(() -> friendService.respondToFriendRequest(
                new FriendRequestResponseDTO("not-a-uuid", true)))
                .isInstanceOf(InvalidUUIDException.class);
    }

    @Test
    void respondToFriendRequestShouldThrowWhenRequestNotFound() {
        setupSecurityContext();
        UUID requestId = UUID.randomUUID();
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.respondToFriendRequest(
                new FriendRequestResponseDTO(requestId.toString(), true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void respondToFriendRequestShouldThrowWhenCurrentUserIsNotReceiver() {
        setupSecurityContext();
        UUID requestId = UUID.randomUUID();
        UserEntity thirdUser = new UserEntity("charlie", "hash");
        FriendRequestEntity request = new FriendRequestEntity(otherUser, thirdUser.getId());
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(thirdUser.getId())).thenReturn(Optional.of(thirdUser));

        assertThatThrownBy(() -> friendService.respondToFriendRequest(
                new FriendRequestResponseDTO(requestId.toString(), true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void respondToFriendRequestShouldSaveFriendshipWhenAccepted() {
        setupSecurityContext();
        UUID requestId = UUID.randomUUID();
        FriendRequestEntity request = new FriendRequestEntity(otherUser, currentUser.getId());
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

        friendService.respondToFriendRequest(new FriendRequestResponseDTO(requestId.toString(), true));

        verify(friendRepository).save(any(FriendEntity.class));
        verify(friendRequestRepository).deleteById(requestId);
    }

    @Test
    void respondToFriendRequestShouldOnlyDeleteRequestWhenDeclined() {
        setupSecurityContext();
        UUID requestId = UUID.randomUUID();
        FriendRequestEntity request = new FriendRequestEntity(otherUser, currentUser.getId());
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

        friendService.respondToFriendRequest(new FriendRequestResponseDTO(requestId.toString(), false));

        verify(friendRepository, never()).save(any());
        verify(friendRequestRepository).deleteById(requestId);
    }

    // --- Get Friend Requests ---

    @Test
    void getFriendRequestsShouldThrowWhenPrincipalIsNull() {
        assertThatThrownBy(() -> friendService.getFriendRequests())
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void getFriendRequestsShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();
        assertThatThrownBy(() -> friendService.getFriendRequests())
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void getFriendRequestsShouldReturnPendingRequests() {
        setupSecurityContext();
        FriendRequestEntity request = new FriendRequestEntity(otherUser, currentUser.getId());
        when(friendRequestRepository.findAllByToUser(currentUser.getId())).thenReturn(List.of(request));

        FriendRequestsDTO result = friendService.getFriendRequests();

        assertThat(result.friendPreviews()).hasSize(1);
        assertThat(result.friendPreviews().getFirst().username()).isEqualTo("alice");
    }

    @Test
    void getFriendRequestsShouldReturnEmptyListWhenNoPending() {
        setupSecurityContext();
        when(friendRequestRepository.findAllByToUser(currentUser.getId())).thenReturn(List.of());

        assertThat(friendService.getFriendRequests().friendPreviews()).isEmpty();
    }

    // --- Remove Friend ---

    @Test
    void removeFriendShouldThrowWhenPrincipalIsNull() {
        assertThatThrownBy(() -> friendService.removeFriend(new UsernameDTO("alice")))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void removeFriendShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();
        assertThatThrownBy(() -> friendService.removeFriend(new UsernameDTO("alice")))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void removeFriendShouldThrowWhenFriendNotFound() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.removeFriend(new UsernameDTO("nobody")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeFriendShouldThrowWhenNotFriends() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> friendService.removeFriend(new UsernameDTO("alice")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeFriendShouldDeleteFriendship() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(true);

        friendService.removeFriend(new UsernameDTO("alice"));

        verify(friendRepository).deleteById(any(FriendId.class));
    }
}
