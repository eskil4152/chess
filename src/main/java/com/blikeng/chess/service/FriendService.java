package com.blikeng.chess.service;

import java.util.List;

import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.AlreadyFriendsException;
import com.blikeng.chess.exception.types.FriendYourselfException;
import com.blikeng.chess.exception.types.NotFoundException;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.blikeng.chess.dto.FriendDTO;
import com.blikeng.chess.dto.UsernameDTO;
import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.FriendId;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;

@Service
public class FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public FriendService(FriendRepository friendRepository, UserRepository userRepository) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
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

    public void addFriend(UsernameDTO usernameDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        if (principal.username().equals(usernameDTO.username())) throw new FriendYourselfException();

        UserEntity user = userRepository.findByUsernameIgnoreCase(principal.username()).orElseThrow(InvalidUserException::new);
        UserEntity friend = userRepository.findByUsernameIgnoreCase(usernameDTO.username()).orElseThrow(NotFoundException::new);

        FriendId friendId = FriendId.generate(user.getId(), friend.getId());

        if (friendRepository.existsById(friendId)) {
            throw new AlreadyFriendsException();
        }

        friendRepository.save(new FriendEntity(
                friendId,
                user,
                friend
            )
        );
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
