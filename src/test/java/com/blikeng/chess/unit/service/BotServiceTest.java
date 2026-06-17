package com.blikeng.chess.unit.service;

import com.blikeng.chess.bot.BotDefinition;
import com.blikeng.chess.bot.BotDifficulty;
import com.blikeng.chess.bot.BotService;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.*;
import com.blikeng.chess.events.MatchStartedEvent;
import com.blikeng.chess.events.MoveMadeEvent;
import java.util.Set;
import com.blikeng.chess.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotServiceTest {

    @Mock GameService gameService;
    @InjectMocks BotService botService;

    private ExecutorService executor;

    private static final UUID BOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        executor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(botService, "executor", executor);
    }

    // --- isBot ---

    @Test
    void isBotShouldReturnTrueForBotId() {
        assertThat(botService.isBot(BOT_ID)).isTrue();
    }

    @Test
    void isBotShouldReturnFalseForRegularId() {
        assertThat(botService.isBot(UUID.randomUUID())).isFalse();
    }

    // --- getBot ---

    @Test
    void getBotShouldReturnCorrectDefinitionForEasyDifficulty() {
        BotDefinition bot = botService.getBot(BotDifficulty.EASY);
        assertThat(bot.difficulty()).isEqualTo(BotDifficulty.EASY);
        assertThat(bot.username()).isEqualTo("Bot-Easy");
    }

    @Test
    void getBotShouldReturnCorrectDefinitionForMediumDifficulty() {
        BotDefinition bot = botService.getBot(BotDifficulty.MEDIUM);
        assertThat(bot.difficulty()).isEqualTo(BotDifficulty.MEDIUM);
        assertThat(bot.username()).isEqualTo("Bot-Medium");
    }

    @Test
    void getBotShouldReturnCorrectDefinitionForHardDifficulty() {
        BotDefinition bot = botService.getBot(BotDifficulty.HARD);
        assertThat(bot.difficulty()).isEqualTo(BotDifficulty.HARD);
        assertThat(bot.username()).isEqualTo("Bot-Hard");
    }

    // --- onMatchStarted ---

    @Test
    void onMatchStartedShouldScheduleMoveWhenBotIsWhite() {
        MatchStartedEvent event = new MatchStartedEvent(UUID.randomUUID(), BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800);

        botService.onMatchStarted(event);

        verify(executor).submit(any(Runnable.class));
    }

    @Test
    void onMatchStartedShouldNotScheduleMoveWhenBotIsBlack() {
        MatchStartedEvent event = new MatchStartedEvent(UUID.randomUUID(), PLAYER_ID, "player", BOT_ID, "Bot-Easy", 800, 800);

        botService.onMatchStarted(event);

        verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    void onMatchStartedShouldNotScheduleMoveWhenNeitherIsBot() {
        MatchStartedEvent event = new MatchStartedEvent(UUID.randomUUID(), PLAYER_ID, "p1", UUID.randomUUID(), "p2", 800, 800);

        botService.onMatchStarted(event);

        verify(executor, never()).submit(any(Runnable.class));
    }

    // --- onMoveMade ---

    @Test
    void onMoveMadeShouldScheduleMoveWhenWhiteTurnAndWhiteIsBot() {
        MoveMadeEvent event = new MoveMadeEvent(UUID.randomUUID(), BOT_ID, PLAYER_ID, "e7e5", true, 0, Set.of());

        botService.onMoveMade(event);

        verify(executor).submit(any(Runnable.class));
    }

    @Test
    void onMoveMadeShouldScheduleMoveWhenBlackTurnAndBlackIsBot() {
        MoveMadeEvent event = new MoveMadeEvent(UUID.randomUUID(), PLAYER_ID, BOT_ID, "e2e4", false, 0, Set.of());

        botService.onMoveMade(event);

        verify(executor).submit(any(Runnable.class));
    }

    @Test
    void onMoveMadeShouldNotScheduleMoveWhenNextPlayerIsHuman() {
        MoveMadeEvent event = new MoveMadeEvent(UUID.randomUUID(), PLAYER_ID, UUID.randomUUID(), "e2e4", true, 0, Set.of());

        botService.onMoveMade(event);

        verify(executor, never()).submit(any(Runnable.class));
    }

    // --- scheduleBotMove (Runnable) ---

    @Test
    void scheduledRunnableShouldCallMakeMoveWhenGameIsActive() {
        UUID gameId = UUID.randomUUID();
        Game game = new Game(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", true);
        when(gameService.getActiveGame(BOT_ID)).thenReturn(Optional.of(game));

        MatchStartedEvent event = new MatchStartedEvent(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800);
        botService.onMatchStarted(event);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(captor.capture());
        captor.getValue().run();

        verify(gameService).makeMove(eq(BOT_ID), any(WsMoveDTO.class));
    }

    @Test
    void scheduledRunnableShouldNotCallMakeMoveWhenGameNoLongerActive() {
        UUID gameId = UUID.randomUUID();
        when(gameService.getActiveGame(BOT_ID)).thenReturn(Optional.empty());

        botService.onMatchStarted(new MatchStartedEvent(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(captor.capture());
        captor.getValue().run();

        verify(gameService, never()).makeMove(any(), any());
    }

    @Test
    void scheduledRunnableShouldNotCallMakeMoveWhenNoLegalMoves() {
        UUID gameId = UUID.randomUUID();
        // Stalemate: white king at a1, all escape squares covered, not in check
        Game game = new Game(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", true);
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                game.getBoard().setPiece(r, c, null);
        game.getBoard().setPiece(0, 0, new King(Color.WHITE));
        game.getBoard().setPiece(7, 4, new King(Color.BLACK));
        game.getBoard().setPiece(7, 0, new Rook(Color.BLACK)); // covers a-file → a2 attacked
        game.getBoard().setPiece(0, 2, new Queen(Color.BLACK)); // covers b1, b2 and rank 1
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 4));
        when(gameService.getActiveGame(BOT_ID)).thenReturn(Optional.of(game));

        botService.onMatchStarted(new MatchStartedEvent(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(captor.capture());
        captor.getValue().run();

        verify(gameService, never()).makeMove(any(), any());
    }

    @Test
    void scheduledRunnableShouldAppendPromoCharWhenBotPromotes() {
        UUID gameId = UUID.randomUUID();
        Game game = new Game(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", true);
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                game.getBoard().setPiece(r, c, null);
        Pawn pawn = new Pawn(Color.WHITE); pawn.setMoved();
        game.getBoard().setPiece(6, 3, pawn);          // d7 — one step from promotion
        game.getBoard().setPiece(0, 4, new King(Color.WHITE));
        game.getBoard().setPiece(7, 4, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 4));
        game.setBlackKingPosition(new Position(7, 4));
        when(gameService.getActiveGame(BOT_ID)).thenReturn(Optional.of(game));

        botService.onMatchStarted(new MatchStartedEvent(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(captor.capture());
        captor.getValue().run();

        ArgumentCaptor<WsMoveDTO> moveCaptor = ArgumentCaptor.forClass(WsMoveDTO.class);
        verify(gameService).makeMove(eq(BOT_ID), moveCaptor.capture());
        assertThat(moveCaptor.getValue().move()).hasSize(5); // e.g. "d7d8q"
    }

    @Test
    void scheduledRunnableShouldHandleInterruptedException() {
        UUID gameId = UUID.randomUUID();
        botService.onMatchStarted(new MatchStartedEvent(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(captor.capture());

        Thread.currentThread().interrupt(); // pre-interrupt so Thread.sleep throws immediately
        captor.getValue().run();
        Thread.interrupted(); // clear flag to not affect subsequent tests

        verify(gameService, never()).makeMove(any(), any());
    }

    @Test
    void scheduledRunnableShouldLogAndSwallowGeneralException() {
        UUID gameId = UUID.randomUUID();
        when(gameService.getActiveGame(BOT_ID)).thenThrow(new RuntimeException("boom"));

        botService.onMatchStarted(new MatchStartedEvent(gameId, BOT_ID, "Bot-Easy", PLAYER_ID, "player", 800, 800));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(captor.capture());
        assertThatCode(() -> captor.getValue().run()).doesNotThrowAnyException();
        verify(gameService, never()).makeMove(any(), any());
    }
}
