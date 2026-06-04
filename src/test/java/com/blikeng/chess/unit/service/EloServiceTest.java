package com.blikeng.chess.unit.service;

import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.service.EloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks EloService eloService;

    private UserEntity white;
    private UserEntity black;

    @BeforeEach
    void setup() {
        white = new UserEntity("white", "hash");
        black = new UserEntity("black", "hash");
        lenient().when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        lenient().when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
    }

    private int[] play(TimeControl tc, GameStatus status) {
        return eloService.updateUserElo(tc, white.getId(), black.getId(), status);
    }

    @Test
    void shouldReturnEmptyArrayForOngoingGame() {
        assertThat(play(TimeControl.RAPID_10_0, GameStatus.ONGOING)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowWhenWhiteNotFound() {
        when(userRepository.findById(white.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenBlackNotFound() {
        when(userRepository.findById(black.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void whiteWinAtEqualEloShouldGain20WithNewPlayerKFactor() {
        int[] result = play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(result[0]).isEqualTo(820);
        assertThat(result[1]).isEqualTo(780);
    }

    @Test
    void drawAtEqualEloShouldNotChangeElo() {
        int[] result = play(TimeControl.RAPID_10_0, GameStatus.DRAW);

        assertThat(result[0]).isEqualTo(800);
        assertThat(result[1]).isEqualTo(800);
    }

    @Test
    void blackWinAtEqualEloShouldGain20WithNewPlayerKFactor() {
        int[] result = play(TimeControl.RAPID_10_0, GameStatus.BLACK_WIN);

        assertThat(result[0]).isEqualTo(780);
        assertThat(result[1]).isEqualTo(820);
    }

    @Test
    void returnValueShouldContainNewElosInWhiteBlackOrder() {
        int[] result = play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo(white.getRapidElo());
        assertThat(result[1]).isEqualTo(black.getRapidElo());
    }

    @Test
    void establishedPlayerShouldUseK20() {
        white.setRapidGames(30);
        int[] result = play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(result[0]).isEqualTo(810);
    }

    @Test
    void been2400PlayerShouldUseK10() {
        white.setRapidGames(100);
        white.setBeen2400Rapid(true);
        int[] result = play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(result[0]).isEqualTo(805);
    }

    @Test
    void shouldSetBeen2400WhenEloExceeds2399() {
        white.setRapidGames(30);
        white.setRapidElo(2395);
        black.setRapidElo(2395);

        play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(white.isBeen2400Rapid()).isTrue();
    }

    @Test
    void shouldNotSetBeen2400WhenEloBelow2400() {
        play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(white.isBeen2400Rapid()).isFalse();
    }

    @Test
    void whiteWinShouldIncrementWhiteWinsAndBlackLosses() {
        play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(white.getRapidWins()).isEqualTo(1);
        assertThat(white.getRapidLosses()).isEqualTo(0);
        assertThat(black.getRapidWins()).isEqualTo(0);
        assertThat(black.getRapidLosses()).isEqualTo(1);
    }

    @Test
    void blackWinShouldIncrementBlackWinsAndWhiteLosses() {
        play(TimeControl.RAPID_10_0, GameStatus.BLACK_WIN);

        assertThat(black.getRapidWins()).isEqualTo(1);
        assertThat(black.getRapidLosses()).isEqualTo(0);
        assertThat(white.getRapidWins()).isEqualTo(0);
        assertThat(white.getRapidLosses()).isEqualTo(1);
    }

    @Test
    void drawShouldNotIncrementWinsOrLosses() {
        play(TimeControl.RAPID_10_0, GameStatus.DRAW);

        assertThat(white.getRapidWins()).isEqualTo(0);
        assertThat(white.getRapidLosses()).isEqualTo(0);
        assertThat(black.getRapidWins()).isEqualTo(0);
        assertThat(black.getRapidLosses()).isEqualTo(0);
    }

    @Test
    void gamesShouldIncrementForBothPlayersRegardlessOfResult() {
        play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(white.getRapidGames()).isEqualTo(1);
        assertThat(black.getRapidGames()).isEqualTo(1);
    }

    @Test
    void shouldSaveBothPlayersAfterUpdate() {
        play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        verify(userRepository).save(white);
        verify(userRepository).save(black);
    }

    @Test
    void bulletGameShouldUpdateBulletStatsOnly() {
        play(TimeControl.BULLET_1_0, GameStatus.WHITE_WIN);

        assertThat(white.getBulletElo()).isNotEqualTo(800);
        assertThat(white.getRapidElo()).isEqualTo(800);
        assertThat(white.getBlitzElo()).isEqualTo(800);
        assertThat(white.getClassicalElo()).isEqualTo(800);
    }

    @Test
    void blitzGameShouldUpdateBlitzStatsOnly() {
        play(TimeControl.BLITZ_3_0, GameStatus.WHITE_WIN);

        assertThat(white.getBlitzElo()).isNotEqualTo(800);
        assertThat(white.getBulletElo()).isEqualTo(800);
        assertThat(white.getRapidElo()).isEqualTo(800);
        assertThat(white.getClassicalElo()).isEqualTo(800);
    }

    @Test
    void rapidGameShouldUpdateRapidStatsOnly() {
        play(TimeControl.RAPID_10_0, GameStatus.WHITE_WIN);

        assertThat(white.getRapidElo()).isNotEqualTo(800);
        assertThat(white.getBulletElo()).isEqualTo(800);
        assertThat(white.getBlitzElo()).isEqualTo(800);
        assertThat(white.getClassicalElo()).isEqualTo(800);
    }

    @Test
    void classicalGameShouldUpdateClassicalStatsOnly() {
        play(TimeControl.CLASSICAL_60_0, GameStatus.WHITE_WIN);

        assertThat(white.getClassicalElo()).isNotEqualTo(800);
        assertThat(white.getBulletElo()).isEqualTo(800);
        assertThat(white.getBlitzElo()).isEqualTo(800);
        assertThat(white.getRapidElo()).isEqualTo(800);
    }
}
