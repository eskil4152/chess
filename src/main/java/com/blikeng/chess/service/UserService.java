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
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.PasswordService;
import org.springframework.stereotype.Service;

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
            user.getBulletElo(),
            user.getBulletGames(),
            user.getBlitzElo(),
            user.getBlitzGames(),
            user.getRapidElo(),
            user.getRapidGames(),
            isFriend
        );
    }

    public int[] updateUserElo(TimeControl timeControl, UUID whiteId, UUID blackId, GameStatus status){
        return switch (status) {
            case WHITE_WIN -> updateElo(timeControl, whiteId, blackId, true, false);
            case BLACK_WIN -> updateElo(timeControl, whiteId, blackId, false, false);
            case DRAW -> updateElo(timeControl, whiteId, blackId, false, true);
            default -> new int[0];
        };
    }

    private int[] updateElo(TimeControl timeControl, UUID whiteId, UUID blackId, boolean whiteWin, boolean draw){
        UserEntity white = userRepository.findById(whiteId).orElseThrow(UserNotFoundException::new);
        UserEntity black = userRepository.findById(blackId).orElseThrow(UserNotFoundException::new);

        double drawScore = draw ? 0.5 : 0.0;
        double whiteScore = whiteWin ? 1.0 : drawScore;
        double blackScore = 1.0 - whiteScore;

        int[] elos = switch (timeControl.type()) {
            case BULLET -> handleBulletElo(white, black, whiteScore, blackScore);
            case BLITZ -> handleBlitzElo(white, black, whiteScore, blackScore);
            case RAPID -> handleRapidElo(white, black, whiteScore, blackScore);
            case CLASSICAL -> handleClassicalElo(white, black, whiteScore, blackScore);
        };

        userRepository.save(white);
        userRepository.save(black);

        return elos;
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

    private int[] handleBulletElo(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getBulletElo(), black.getBulletElo(), whiteScore, getKFactor(white.getBulletGames(), white.isBeen2400Bullet()));
        int blackElo = calculateNewElo(black.getBulletElo(), white.getBulletElo(), blackScore, getKFactor(black.getBulletGames(), black.isBeen2400Bullet()));

        white.setBulletGames(white.getBulletGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Bullet(true);
        white.setBulletElo(whiteElo);

        black.setBulletGames(black.getBulletGames() + 1);
        if (blackElo > 2399) black.setBeen2400Bullet(true);
        black.setBulletElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }

    private int[] handleBlitzElo(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getBlitzElo(), black.getBlitzElo(), whiteScore, getKFactor(white.getBlitzGames(), white.isBeen2400Blitz()));
        int blackElo = calculateNewElo(black.getBlitzElo(), white.getBlitzElo(), blackScore, getKFactor(black.getBlitzGames(), black.isBeen2400Blitz()));

        white.setBlitzGames(white.getBlitzGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Blitz(true);
        white.setBlitzElo(whiteElo);

        black.setBlitzGames(black.getBlitzGames() + 1);
        if (blackElo > 2399) black.setBeen2400Blitz(true);
        black.setBlitzElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }

    private int[] handleRapidElo(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getRapidElo(), black.getRapidElo(), whiteScore, getKFactor(white.getRapidGames(), white.isBeen2400Rapid()));
        int blackElo = calculateNewElo(black.getRapidElo(), white.getRapidElo(), blackScore, getKFactor(black.getRapidGames(), black.isBeen2400Rapid()));

        white.setRapidGames(white.getRapidGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Rapid(true);
        white.setRapidElo(whiteElo);

        black.setRapidGames(black.getRapidGames() + 1);
        if (blackElo > 2399) black.setBeen2400Rapid(true);
        black.setRapidElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }

    private int[] handleClassicalElo(UserEntity white, UserEntity black, double whiteScore, double blackScore){
        int whiteElo = calculateNewElo(white.getClassicalElo(), black.getClassicalElo(), whiteScore, getKFactor(white.getClassicalGames(), white.isBeen2400Classical()));
        int blackElo = calculateNewElo(black.getClassicalElo(), white.getClassicalElo(), blackScore, getKFactor(black.getClassicalGames(), black.isBeen2400Classical()));

        white.setClassicalGames(white.getClassicalGames() + 1);
        if (whiteElo > 2399) white.setBeen2400Classical(true);
        white.setClassicalElo(whiteElo);

        black.setClassicalGames(black.getClassicalGames() + 1);
        if (blackElo > 2399) black.setBeen2400Classical(true);
        black.setClassicalElo(blackElo);

        return new int[]{whiteElo, blackElo};
    }
}
