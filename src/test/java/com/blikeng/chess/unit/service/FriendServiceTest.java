package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.FriendDTO;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.FriendId;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.FriendService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock FriendRepository friendRepository;
    @Mock UserRepository userRepository;
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
        var auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setupSecurityContextWithNullUserId() {
        var principal = new JwtPrincipal(null, currentUser.getUsername(), UserRole.USER);
        var auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
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

    // --- Add Friend ---

    @Test
    void addFriendShouldThrowWhenPrincipalIsNull() {
        UsernameDTO usernameDTO = new UsernameDTO("alice");

        assertThatThrownBy(() -> friendService.addFriend(usernameDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void addFriendShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();

        UsernameDTO usernameDTO = new UsernameDTO("alice");

        assertThatThrownBy(() -> friendService.addFriend(usernameDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void addFriendShouldThrowWhenFriendNotFound() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        UsernameDTO usernameDTO = new UsernameDTO("nobody");

        assertThatThrownBy(() -> friendService.addFriend(usernameDTO))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addFriendShouldThrowWhenAlreadyFriends() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(true);

        UsernameDTO usernameDTO = new UsernameDTO("alice");

        assertThatThrownBy(() -> friendService.addFriend(usernameDTO))
                .isInstanceOf(AlreadyFriendsException.class);
    }

    @Test
    void addFriendShouldThrowWhenAddingSelf() {
        setupSecurityContext();
        UsernameDTO usernameDTO = new UsernameDTO("me");

        assertThatThrownBy(() -> friendService.addFriend(usernameDTO))
                .isInstanceOf(FriendYourselfException.class);

        verify(friendRepository, never()).save(any());
    }

    @Test
    void addFriendShouldSaveFriendship() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(false);

        friendService.addFriend(new UsernameDTO("alice"));

        verify(friendRepository).save(any(FriendEntity.class));
    }

    // --- Remove Friend ---

    @Test
    void removeFriendShouldThrowWhenPrincipalIsNull() {
        UsernameDTO usernameDTO = new UsernameDTO("alice");

        assertThatThrownBy(() -> friendService.removeFriend(usernameDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void removeFriendShouldThrowWhenUserIdIsNull() {
        setupSecurityContextWithNullUserId();
        UsernameDTO usernameDTO = new UsernameDTO("alice");

        assertThatThrownBy(() -> friendService.removeFriend(usernameDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void removeFriendShouldThrowWhenFriendNotFound() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        UsernameDTO usernameDTO = new UsernameDTO("nobody");

        assertThatThrownBy(() -> friendService.removeFriend(usernameDTO))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeFriendShouldThrowWhenNotFriends() {
        setupSecurityContext();
        when(userRepository.findByUsernameIgnoreCase("me")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(otherUser));
        when(friendRepository.existsById(any())).thenReturn(false);

        UsernameDTO usernameDTO = new UsernameDTO("alice");

        assertThatThrownBy(() -> friendService.removeFriend(usernameDTO))
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