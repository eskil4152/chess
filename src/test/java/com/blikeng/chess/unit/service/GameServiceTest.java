package com.blikeng.chess.unit.service;

import com.blikeng.chess.bot.BotDefinition;
import com.blikeng.chess.bot.BotDifficulty;
import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.dto.websocket.WsResignDTO;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.King;
import com.blikeng.chess.model.piece.Pawn;
import com.blikeng.chess.model.piece.Queen;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.service.NotificationService;
import com.blikeng.chess.events.MatchEndedEvent;
import com.blikeng.chess.events.MatchStartedEvent;
import com.blikeng.chess.events.MoveMadeEvent;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.game.GameService;
import com.blikeng.chess.service.game.GameClockService;
import com.blikeng.chess.service.game.GameCompletionService;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameCreationService;
import com.blikeng.chess.service.game.GameViewService;
import com.blikeng.chess.service.StatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock GameRepository gameRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock NotificationService notificationService;
    @Mock StatsService statsService;

    private GameService gameService;
    private GameCreationService gameCreationService;
    private GameViewService gameViewService;
    private ActiveGameStore activeGameStore;

    private UserEntity white;
    private UserEntity black;
    private GameEntity savedEntity;

    @BeforeEach
    void setup() {
        white = new UserEntity("white", "h");
        black = new UserEntity("black", "h");
        savedEntity = new GameEntity(white, black, GameStatus.ONGOING, Instant.now(), "blitz", null);

        // Real collaborators wired with the mocked infrastructure, so behavior
        // assertions (saves / events / stats / cleanup) still flow through.
        GameClockService clockService = new GameClockService();
        activeGameStore = new ActiveGameStore();
        GameCompletionService completionService = new GameCompletionService(eventPublisher, gameRepository, statsService, activeGameStore, clockService);
        gameService = new GameService(eventPublisher, notificationService, clockService, completionService, activeGameStore);
        gameCreationService = new GameCreationService(gameRepository, activeGameStore, clockService, eventPublisher, completionService);
        gameViewService = new GameViewService(activeGameStore);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubSave() {
        when(gameRepository.save(any())).thenReturn(savedEntity);
    }

    @SuppressWarnings("unchecked")
    private Game beginAndGetGame() {
        stubSave();
        gameCreationService.beginGame(white, black, TimeControl.BLITZ_3_0);
        ConcurrentHashMap<UUID, Game> gamesMap = (ConcurrentHashMap<UUID, Game>)
                ReflectionTestUtils.getField(activeGameStore, "games");
        assert gamesMap != null;
        return gamesMap.values().iterator().next();
    }

    private static final BotDefinition BOT = new BotDefinition(
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "Bot-Easy", BotDifficulty.EASY);

    @SuppressWarnings("unchecked")
    private Game beginBotGameAndGet() {
        gameCreationService.beginBotGame(white, BOT);
        ConcurrentHashMap<UUID, Game> gamesMap = (ConcurrentHashMap<UUID, Game>)
                ReflectionTestUtils.getField(activeGameStore, "games");
        assert gamesMap != null;
        return gamesMap.values().iterator().next();
    }


    // --- Begin Game ---
    @Test
    void beginGameShouldSaveEntityAndPublishEvent() {
        stubSave();
        gameCreationService.beginGame(white, black, TimeControl.BLITZ_3_0);

        verify(gameRepository).save(any());
        ArgumentCaptor<MatchStartedEvent> captor = ArgumentCaptor.forClass(MatchStartedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().whiteUsername()).isIn("white", "black");
        assertThat(captor.getValue().blackUsername()).isIn("white", "black");
        assertThat(captor.getValue().whiteUsername()).isNotEqualTo(captor.getValue().blackUsername());
    }

    @Test
    void beginGameShouldAddBothPlayersToActiveGames() {
        stubSave();
        gameCreationService.beginGame(white, black, TimeControl.BLITZ_3_0);
        assertThat(activeGameStore.isInGame(white.getId())).isTrue();
        assertThat(activeGameStore.isInGame(black.getId())).isTrue();
    }


    // --- Begin Bot Game ---

    @Test
    void beginBotGameShouldNotPersistToDatabase() {
        gameCreationService.beginBotGame(white, BOT);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void beginBotGameShouldPublishMatchStartedEvent() {
        gameCreationService.beginBotGame(white, BOT);
        ArgumentCaptor<MatchStartedEvent> captor = ArgumentCaptor.forClass(MatchStartedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().whiteUsername()).isIn("white", "Bot-Easy");
        assertThat(captor.getValue().blackUsername()).isIn("white", "Bot-Easy");
    }

    @Test
    void beginBotGameShouldAddBothPlayersToActiveGames() {
        gameCreationService.beginBotGame(white, BOT);
        assertThat(activeGameStore.isInGame(white.getId())).isTrue();
        assertThat(activeGameStore.isInGame(BOT.id())).isTrue();
    }

    // --- Get Active Game ---

    @Test
    void getActiveGameShouldReturnGameForWhitePlayer() {
        Game game = beginAndGetGame();
        assertThat(activeGameStore.findByUser(game.getWhiteId())).contains(game);
    }

    @Test
    void getActiveGameShouldReturnGameForBlackPlayer() {
        Game game = beginAndGetGame();
        assertThat(activeGameStore.findByUser(game.getBlackId())).contains(game);
    }

    @Test
    void getActiveGameShouldReturnEmptyWhenPlayerNotInGame() {
        assertThat(activeGameStore.findByUser(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getActiveGameShouldReturnEmptyWhenGameExistsButUserIsNotInIt() {
        beginAndGetGame();
        assertThat(activeGameStore.findByUser(UUID.randomUUID())).isEmpty();
    }

    // --- Is In Game ---
    @Test
    void isInGameShouldReturnFalseWhenPlayerHasNoGame() {
        assertThat(activeGameStore.isInGame(UUID.randomUUID())).isFalse();
    }

    // --- Is In Game (filter branching) ---
    @Test
    void isInGameShouldReturnTrueForWhitePlayer() {
        Game game = beginAndGetGame();
        assertThat(activeGameStore.isInGame(game.getWhiteId())).isTrue();
    }

    @Test
    void isInGameShouldReturnTrueForBlackPlayer() {
        Game game = beginAndGetGame();
        assertThat(activeGameStore.isInGame(game.getBlackId())).isTrue();
    }

    @Test
    void isInGameShouldReturnFalseForUnknownPlayer() {
        beginAndGetGame();
        assertThat(activeGameStore.isInGame(UUID.randomUUID())).isFalse();
    }

    // --- Is User Turn ---
    @Test
    void makeMoveShouldAllowBlackMoveWhenItIsBlacksTurn() {
        Game game = beginAndGetGame();
        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));
        gameService.makeMove(game.getBlackId(), new WsMoveDTO(game.getId().toString(), "e7e5", null, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(3)).publishEvent(captor.capture());
        assertThat(
                captor
                        .getAllValues()
                        .stream()
                        .filter(MoveMadeEvent.class::isInstance)
                        .count()
        ).isEqualTo(2);
    }

    // --- Make Move ---
    @Test
    void makeMoveShouldThrowWhenGameNotFound() {
        WsMoveDTO dto = new WsMoveDTO(UUID.randomUUID().toString(), "e2e4", null, null);
        UUID whiteId = white.getId();
        assertThatThrownBy(() -> gameService.makeMove(whiteId, dto))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void makeMoveShouldThrowOnInvalidUUIDFormat() {
        WsMoveDTO dto = new WsMoveDTO("not-valid-uuid", "e2e4", null, null);
        UUID whiteId = white.getId();
        assertThatThrownBy(() -> gameService.makeMove(whiteId, dto))
                .isInstanceOf(InvalidUUIDException.class);
    }

    @Test
    void makeMoveShouldThrowWhenMoveTooShort() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2", null, null);
        UUID whiteId = game.getWhiteId();
        assertThatThrownBy(() -> gameService.makeMove(whiteId, dto))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void makeMoveShouldReturnEarlyWhenNotPlayersTurn() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e7e5", null, null);
        gameService.makeMove(game.getBlackId(), dto);
        verify(eventPublisher, times(1)).publishEvent(any(MatchStartedEvent.class));
    }

    @Test
    void makeMoveShouldPublishMoveMadeEventForValidMove() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2e4", null, null);
        gameService.makeMove(game.getWhiteId(), dto);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MoveMadeEvent.class::isInstance);
    }

    @Test
    void makeMoveShouldNotPublishEventForInvalidMove() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2d3", null, null);
        gameService.makeMove(game.getWhiteId(), dto);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(1)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).noneMatch(MoveMadeEvent.class::isInstance);
    }

    @Test
    void makeMoveShouldParsePromotionChar() {
        Game game = beginAndGetGame();

        Pawn pawn = new Pawn(Color.WHITE);
        pawn.setMoved();
        game.getBoard().setPiece(6, 2, null);  // clear black pawn at c7
        game.getBoard().setPiece(6, 2, pawn);  // place white pawn at c7
        game.getBoard().setPiece(7, 2, null);  // clear black bishop at c8
        game.getBoard().setPiece(7, 4, null);  // clear black king at e8
        game.getBoard().setPiece(7, 7, new King(Color.BLACK));
        game.setBlackKingPosition(new Position(7, 7));

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "c7c8q", null, null));

        assertThat(game.getBoard().getPiece(7, 2)).isInstanceOf(Queen.class);
    }


    // --- Game End ---
    @Test
    void makeMoveShouldPublishMatchEndedWithStalemateWhenDraw() {
        Game game = beginAndGetGame();

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                game.getBoard().setPiece(r, c, null);

        // White queen at d7, white king at c6, black king at a8
        // d7c7 → queen to c7, covering all black king escape squares → stalemate
        game.getBoard().setPiece(6, 3, new Queen(Color.WHITE)); // d7
        game.getBoard().setPiece(5, 2, new King(Color.WHITE));  // c6
        game.getBoard().setPiece(7, 0, new King(Color.BLACK));  // a8
        game.setWhiteKingPosition(new Position(5, 2));
        game.setBlackKingPosition(new Position(7, 0));

        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "d7c7", null, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MatchEndedEvent.class::isInstance);
        assertThat(activeGameStore.isInGame(white.getId())).isFalse();
    }

    @Test
    void makeMoveShouldPublishMatchEndedAndRemoveGameWhenOver() {
        Game game = beginAndGetGame();

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                game.getBoard().setPiece(r, c, null);

        game.getBoard().setPiece(4, 6, new Queen(Color.WHITE));
        game.getBoard().setPiece(5, 5, new King(Color.WHITE));
        game.getBoard().setPiece(7, 7, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(5, 5));
        game.setBlackKingPosition(new Position(7, 7));

        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "g5g7", null, null));

        verify(gameRepository).findById(game.getId());
        verify(gameRepository, atLeast(2)).save(any());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MatchEndedEvent.class::isInstance);
        assertThat(activeGameStore.isInGame(game.getWhiteId())).isFalse();
    }


    // --- Bot Game End (isBotGame checks) ---

    @Test
    void makeMoveShouldNotPersistOrUpdateEloWhenBotGameEnds() {
        Game game = beginBotGameAndGet();

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                game.getBoard().setPiece(r, c, null);

        game.getBoard().setPiece(4, 6, new Queen(Color.WHITE));
        game.getBoard().setPiece(5, 5, new King(Color.WHITE));
        game.getBoard().setPiece(7, 7, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(5, 5));
        game.setBlackKingPosition(new Position(7, 7));

        UUID moverId = game.getWhiteId();
        gameService.makeMove(moverId, new WsMoveDTO(game.getId().toString(), "g5g7", null, null));

        verify(gameRepository, never()).findById(any());
        verify(statsService, never()).updateUserStatsAndReturnNewElos(any(), any(), any(), any());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(1)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MatchEndedEvent.class::isInstance);
    }

    @Test
    void resignGameShouldNotPersistOrUpdateEloWhenBotGame() {
        Game game = beginBotGameAndGet();

        gameService.resignGame(game.getWhiteId(), new WsResignDTO(game.getId().toString()));

        verify(gameRepository, never()).findById(any());
        verify(statsService, never()).updateUserStatsAndReturnNewElos(any(), any(), any(), any());
        assertThat(activeGameStore.isInGame(game.getWhiteId())).isFalse();
    }

    @Test
    void handleDrawShouldNotPersistOrUpdateEloWhenBotGameEndsInAgreement() {
        Game game = beginBotGameAndGet();
        UUID humanId = game.getWhiteId().equals(white.getId()) ? game.getWhiteId() : game.getBlackId();
        UUID botId = game.getWhiteId().equals(white.getId()) ? game.getBlackId() : game.getWhiteId();

        gameService.handleDraw(humanId, new WsDrawDTO(game.getId().toString()));
        gameService.handleDraw(botId, new WsDrawDTO(game.getId().toString()));

        verify(gameRepository, never()).findById(any());
        verify(statsService, never()).updateUserStatsAndReturnNewElos(any(), any(), any(), any());
        assertThat(activeGameStore.isInGame(humanId)).isFalse();
    }

    // --- Resign ---
    @Test
    void resignGameShouldThrowWhenUserNotInGame() {
        Game game = beginAndGetGame();
        WsResignDTO dto = new WsResignDTO(game.getId().toString());
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> gameService.resignGame(randomId, dto))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void resignGameShouldEndGameWithBlackWinWhenWhiteResigns() {
        Game game = beginAndGetGame();
        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.resignGame(game.getWhiteId(), new WsResignDTO(game.getId().toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.BLACK_WIN);
        assertThat(activeGameStore.isInGame(game.getWhiteId())).isFalse();
    }

    @Test
    void resignGameShouldEndGameWithWhiteWinWhenBlackResigns() {
        Game game = beginAndGetGame();
        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.resignGame(game.getBlackId(), new WsResignDTO(game.getId().toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.WHITE_WIN);
    }

    // --- Draw ---
    @Test
    void handleDrawShouldThrowWhenUserNotInGame() {
        Game game = beginAndGetGame();
        WsDrawDTO dto = new WsDrawDTO(game.getId().toString());
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> gameService.handleDraw(randomId, dto))
                .isInstanceOf(NotAllowedException.class);
    }

    @Test
    void handleDrawShouldSendOfferToBlackWhenWhiteOffers() {
        Game game = beginAndGetGame();

        gameService.handleDraw(game.getWhiteId(), new WsDrawDTO(game.getId().toString()));

        verify(notificationService).sendDrawOffer(game.getId(), game.getBlackId());
        assertThat(activeGameStore.isInGame(game.getWhiteId())).isTrue();
    }

    @Test
    void handleDrawShouldSendOfferToWhiteWhenBlackOffers() {
        Game game = beginAndGetGame();

        gameService.handleDraw(game.getBlackId(), new WsDrawDTO(game.getId().toString()));

        verify(notificationService).sendDrawOffer(game.getId(), game.getWhiteId());
        assertThat(activeGameStore.isInGame(game.getBlackId())).isTrue();
    }

    @Test
    void handleDrawShouldEndGameWhenBothPlayersAccept() {
        Game game = beginAndGetGame();
        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.handleDraw(white.getId(), new WsDrawDTO(game.getId().toString()));
        gameService.handleDraw(black.getId(), new WsDrawDTO(game.getId().toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.DRAW);
        assertThat(activeGameStore.isInGame(white.getId())).isFalse();
    }

    // --- Restore Game State ---
    private void setupSecurityContext(UUID userId) {
        var principal = new JwtPrincipal(userId, "testuser", UserRole.USER);
        var auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void restoreGameStateShouldReturnStateWhenPlayerIsInGame() {
        beginAndGetGame();
        setupSecurityContext(white.getId());
        GameStateDTO dto = gameViewService.restoreGameState();
        assertThat(dto.whiteId()).isIn(white.getId(), black.getId());
    }

    @Test
    void restoreGameStateShouldReturnStateForBlackPlayer() {
        beginAndGetGame();
        setupSecurityContext(black.getId());
        GameStateDTO dto = gameViewService.restoreGameState();
        assertThat(dto.blackId()).isIn(white.getId(), black.getId());
    }

    @Test
    void restoreGameStateShouldThrowWhenPlayerNotInGame() {
        setupSecurityContext(UUID.randomUUID());
        assertThatThrownBy(() -> gameViewService.restoreGameState())
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void restoreGameStateShouldThrowWhenExistingGameDoesNotBelongToUser() {
        beginAndGetGame();
        setupSecurityContext(UUID.randomUUID());
        assertThatThrownBy(() -> gameViewService.restoreGameState())
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void restoreGameStateShouldThrowWhenNotAuthenticated() {
        assertThatThrownBy(() -> gameViewService.restoreGameState())
                .isInstanceOf(InvalidUserException.class);
    }

    // --- Timeout (handleTime) ---

    @Test
    void makeMoveShouldEndGameWithTimeoutWhenWhiteFlagsOnMove() {
        Game game = beginAndGetGame();
        game.setWhiteRemainingMs(0);

        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(1)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.BLACK_WIN);
        assertThat(activeGameStore.isInGame(game.getWhiteId())).isFalse();
    }

    @Test
    void makeMoveShouldEndGameWithTimeoutWhenBlackFlagsOnMove() {
        Game game = beginAndGetGame();
        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));

        game.setBlackRemainingMs(0);

        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getBlackId(), new WsMoveDTO(game.getId().toString(), "e7e5", null, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(1)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.WHITE_WIN);
        assertThat(activeGameStore.isInGame(game.getBlackId())).isFalse();
    }

    // --- Clock update (ONGOING branch) ---

    @Test
    void makeMoveShouldResetTurnClockAfterValidMove() {
        Game game = beginAndGetGame();
        long before = System.currentTimeMillis();

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));

        assertThat(game.getTurnStartTime()).isGreaterThanOrEqualTo(before);
    }

    @Test
    @SuppressWarnings("unchecked")
    void makeMoveShouldAddIncrementAfterValidMove() {
        stubSave();
        gameCreationService.beginGame(white, black, TimeControl.BLITZ_3_2);
        ConcurrentHashMap<UUID, Game> gamesMap = (ConcurrentHashMap<UUID, Game>)
                ReflectionTestUtils.getField(activeGameStore, "games");
        assert gamesMap != null;
        Game game = gamesMap.values().iterator().next();
        int initialMs = game.getWhiteRemainingMs();

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));

        assertThat(game.getWhiteRemainingMs()).isGreaterThan(initialMs);
    }

    // --- Scheduled flag check ---

    @Test
    void scheduledFlagCheckShouldFlagPlayerOnTimeout() {
        Game game = beginAndGetGame();
        game.setBlackRemainingMs(100);

        when(gameRepository.findById(any())).thenReturn(Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));

        await().atMost(2, TimeUnit.SECONDS).until(() -> !activeGameStore.isInGame(game.getWhiteId()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(1)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.WHITE_WIN);
    }

    @Test
    void scheduledFlagCheckShouldNotEndAlreadyEndedGame() throws InterruptedException {
        Game game = beginAndGetGame();
        game.setBlackRemainingMs(100);

        when(gameRepository.findById(any())).thenReturn(Optional.of(savedEntity));
        when(statsService.updateUserStatsAndReturnNewElos(any(), any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4", null, null));
        gameService.resignGame(game.getBlackId(), new WsResignDTO(game.getId().toString()));

        Thread.sleep(300);

        verify(statsService, times(1)).updateUserStatsAndReturnNewElos(any(), any(), any(), any());
    }
}