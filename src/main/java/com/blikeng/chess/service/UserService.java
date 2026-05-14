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
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final PasswordService passwordService;

    public UserService(UserRepository userRepository, FriendRepository friendRepository, PasswordService passwordService){
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.passwordService = passwordService;
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

        return new ProfileDTO(
            user.getUsername(),
            user.getBio(),
            user.getAvatarUrl(),
            user.getElo(),
            isFriend
        );
    }

    public int[] updateUserElo(UUID whiteId, UUID blackId, GameStatus status){
        return switch (status) {
            case WHITE_WIN -> updateElo(whiteId, blackId, true, false);
            case BLACK_WIN -> updateElo(whiteId, blackId, false, false);
            case DRAW -> updateElo(whiteId, blackId, false, true);
            default -> new int[0];
        };
    }

    private int[] updateElo(UUID whiteId, UUID blackId, boolean whiteWin, boolean draw){
        UserEntity white = userRepository.findById(whiteId).orElseThrow(UserNotFoundException::new);
        UserEntity black = userRepository.findById(blackId).orElseThrow(UserNotFoundException::new);

        double drawScore = draw ? 0.5 : 0.0;
        double whiteScore = whiteWin ? 1.0 : drawScore;
        double blackScore = 1.0 - whiteScore;

        int whiteElo = calculateNewElo(white.getElo(), black.getElo(), whiteScore, getKFactor(white.getGames(), white.isBeen2400()));
        int blackElo = calculateNewElo(black.getElo(), white.getElo(), blackScore, getKFactor(black.getGames(), black.isBeen2400()));

        white.setGames(white.getGames() + 1);
        if (whiteElo > 2399) white.setBeen2400(true);
        white.setElo(whiteElo);

        black.setGames(black.getGames() + 1);
        if (blackElo > 2399) black.setBeen2400(true);
        black.setElo(blackElo);

        userRepository.save(white);
        userRepository.save(black);

        return new int[]{whiteElo, blackElo};
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

    private int calculateNewElo(int playerElo, int opponentElo, double score, int kFactor) {
        double expected = 1.0 / (1 + Math.pow(10, (opponentElo - playerElo) / 400.0));
        return (int) Math.round(playerElo + kFactor * (score - expected));
    }

    private int getKFactor(int gamesPlayed, boolean been2400) {
        if (been2400) return 10;

        return gamesPlayed >= 30 ? 20 : 40;
    }
}
