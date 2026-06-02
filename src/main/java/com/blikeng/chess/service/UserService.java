package com.blikeng.chess.service;

import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.entity.FriendId;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.BadEditException;
import com.blikeng.chess.exception.types.InvalidPasswordException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final PasswordService passwordService;
    private final GameService gameService;

    public UserService(UserRepository userRepository, FriendRepository friendRepository, PasswordService passwordService, GameService gameService){
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.passwordService = passwordService;
        this.gameService = gameService;
    }

    public ProfileDTO getUser(String username) {
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        if (username == null || username.trim().isBlank()) throw new UserNotFoundException();
        username = username.trim();

        UserEntity user = userRepository.findByUsernameIgnoreCase(username).orElseThrow(UserNotFoundException::new);

        boolean isFriend = false;
        boolean isSelf = username.equals(principal.username());
        if (!isSelf){
            isFriend = friendRepository.existsById(
                FriendId.generate(principal.userId(), user.getId())
            );
        }

        String gameId = gameService.getActiveGame(user.getId())
                .map(g -> g.getId().toString())
                .orElse(null);

        return new ProfileDTO(
            user.getUsername(),
            user.getBio(),
            user.getAvatarUrl(),

            user.getBulletElo(),
            user.getBulletGames(),
            user.getBulletWins(),
            user.getWinPercentage("bullet"),

            user.getBlitzElo(),
            user.getBlitzGames(),
            user.getBlitzWins(),
            user.getWinPercentage("blitz"),

            user.getRapidElo(),
            user.getRapidGames(),
            user.getRapidWins(),
            user.getWinPercentage("rapid"),

            user.getClassicalElo(),
            user.getClassicalGames(),
            user.getClassicalWins(),
            user.getWinPercentage("classical"),

            isFriend,
            gameId
        );
    }

    public void updateUser(ProfileEditDTO profileEditDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        UserEntity user = userRepository.findById(principal.userId()).orElseThrow(InvalidUserException::new);

        if (profileEditDTO.field().isBlank()) throw new BadEditException();

        switch (profileEditDTO.field()) {
            case "bio" -> user.setBio(profileEditDTO.newValue().trim());
            case "avatarUrl" -> user.setAvatarUrl(profileEditDTO.newValue().trim());
            default -> throw new BadEditException();
        }

        userRepository.save(user);
    }

    public void updatePassword(PasswordDTO passwordDTO){
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        if (
            passwordDTO.newPassword().isBlank() ||
            passwordDTO.newPassword().trim().length() > 128 ||
            passwordDTO.newPassword().trim().length() < 8
        ) throw new BadEditException();

        UserEntity user = userRepository.findById(principal.userId()).orElseThrow(InvalidUserException::new);

        if (!passwordService.checkPassword(passwordDTO.oldPassword(), user.getPassword())) throw new InvalidPasswordException();

        user.setPassword(passwordService.hashPassword(passwordDTO.newPassword()));
        userRepository.save(user);
    }
}
