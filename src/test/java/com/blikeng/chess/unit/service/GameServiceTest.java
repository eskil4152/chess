package com.blikeng.chess.unit.service;

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
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock GameRepository gameRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock NotificationService notificationService;
    @Mock UserService userService;
    @InjectMocks GameService gameService;

    private UserEntity white;
    private UserEntity black;
    private GameEntity savedEntity;

    @BeforeEach
    void setup() {
        white = new UserEntity("white", "h");
        black = new UserEntity("black", "h");
        savedEntity = new GameEntity(white, black, GameStatus.ONGOING, Instant.now());
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
        gameService.beginGame(white, black);
        ConcurrentHashMap<UUID, Game> gamesMap = (ConcurrentHashMap<UUID, Game>)
                ReflectionTestUtils.getField(gameService, "games");
        return gamesMap.values().iterator().next();
    }


    // --- Begin Game ---
    @Test
    void beginGameShouldSaveEntityAndPublishEvent() {
        stubSave();
        gameService.beginGame(white, black);

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
        gameService.beginGame(white, black);
        assertThat(gameService.isInGame(white.getId())).isTrue();
        assertThat(gameService.isInGame(black.getId())).isTrue();
    }


    // --- Is In Game ---
    @Test
    void isInGameShouldReturnFalseWhenPlayerHasNoGame() {
        assertThat(gameService.isInGame(UUID.randomUUID())).isFalse();
    }

    // --- Is In Game (filter branching) ---
    @Test
    void isInGameShouldReturnTrueForWhitePlayer() {
        Game game = beginAndGetGame();
        assertThat(gameService.isInGame(game.getWhiteId())).isTrue();
    }

    @Test
    void isInGameShouldReturnTrueForBlackPlayer() {
        Game game = beginAndGetGame();
        assertThat(gameService.isInGame(game.getBlackId())).isTrue();
    }

    @Test
    void isInGameShouldReturnFalseForUnknownPlayer() {
        beginAndGetGame();
        assertThat(gameService.isInGame(UUID.randomUUID())).isFalse();
    }

    // --- Is User Turn ---
    @Test
    void makeMoveShouldAllowBlackMoveWhenItIsBlacksTurn() {
        Game game = beginAndGetGame();
        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "e2e4"));
        gameService.makeMove(game.getBlackId(), new WsMoveDTO(game.getId().toString(), "e7e5"));

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
        WsMoveDTO dto = new WsMoveDTO(UUID.randomUUID().toString(), "e2e4");
        UUID whiteId = white.getId();
        assertThatThrownBy(() -> gameService.makeMove(whiteId, dto))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void makeMoveShouldThrowOnInvalidUUIDFormat() {
        WsMoveDTO dto = new WsMoveDTO("not-valid-uuid", "e2e4");
        UUID whiteId = white.getId();
        assertThatThrownBy(() -> gameService.makeMove(whiteId, dto))
                .isInstanceOf(InvalidUUIDException.class);
    }

    @Test
    void makeMoveShouldThrowWhenMoveTooShort() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2");
        UUID whiteId = game.getWhiteId();
        assertThatThrownBy(() -> gameService.makeMove(whiteId, dto))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void makeMoveShouldReturnEarlyWhenNotPlayersTurn() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e7e5");
        gameService.makeMove(game.getBlackId(), dto);
        verify(eventPublisher, times(1)).publishEvent(any(MatchStartedEvent.class));
    }

    @Test
    void makeMoveShouldPublishMoveMadeEventForValidMove() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2e4");
        gameService.makeMove(game.getWhiteId(), dto);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MoveMadeEvent.class::isInstance);
    }

    @Test
    void makeMoveShouldNotPublishEventForInvalidMove() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2d3");
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

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "c7c8q"));

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
        when(userService.updateUserElo(any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "d7c7"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MatchEndedEvent.class::isInstance);
        assertThat(gameService.isInGame(white.getId())).isFalse();
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
        when(userService.updateUserElo(any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.makeMove(game.getWhiteId(), new WsMoveDTO(game.getId().toString(), "g5g7"));

        verify(gameRepository).findById(game.getId());
        verify(gameRepository, atLeast(2)).save(any());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(MatchEndedEvent.class::isInstance);
        assertThat(gameService.isInGame(game.getWhiteId())).isFalse();
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
        when(userService.updateUserElo(any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.resignGame(game.getWhiteId(), new WsResignDTO(game.getId().toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.BLACK_WIN);
        assertThat(gameService.isInGame(game.getWhiteId())).isFalse();
    }

    @Test
    void resignGameShouldEndGameWithWhiteWinWhenBlackResigns() {
        Game game = beginAndGetGame();
        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(userService.updateUserElo(any(), any(), any())).thenReturn(new int[]{800, 800});

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
        assertThat(gameService.isInGame(game.getWhiteId())).isTrue();
    }

    @Test
    void handleDrawShouldSendOfferToWhiteWhenBlackOffers() {
        Game game = beginAndGetGame();

        gameService.handleDraw(game.getBlackId(), new WsDrawDTO(game.getId().toString()));

        verify(notificationService).sendDrawOffer(game.getId(), game.getWhiteId());
        assertThat(gameService.isInGame(game.getBlackId())).isTrue();
    }

    @Test
    void handleDrawShouldEndGameWhenBothPlayersAccept() {
        Game game = beginAndGetGame();
        when(gameRepository.findById(game.getId())).thenReturn(java.util.Optional.of(savedEntity));
        when(userService.updateUserElo(any(), any(), any())).thenReturn(new int[]{800, 800});

        gameService.handleDraw(white.getId(), new WsDrawDTO(game.getId().toString()));
        gameService.handleDraw(black.getId(), new WsDrawDTO(game.getId().toString()));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e instanceof MatchEndedEvent ev && ev.status() == GameStatus.DRAW);
        assertThat(gameService.isInGame(white.getId())).isFalse();
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
        GameStateDTO dto = gameService.restoreGameState();
        assertThat(dto.whiteId()).isIn(white.getId(), black.getId());
    }

    @Test
    void restoreGameStateShouldReturnStateForBlackPlayer() {
        beginAndGetGame();
        setupSecurityContext(black.getId());
        GameStateDTO dto = gameService.restoreGameState();
        assertThat(dto.blackId()).isIn(white.getId(), black.getId());
    }

    @Test
    void restoreGameStateShouldThrowWhenPlayerNotInGame() {
        setupSecurityContext(UUID.randomUUID());
        assertThatThrownBy(() -> gameService.restoreGameState())
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void restoreGameStateShouldThrowWhenExistingGameDoesNotBelongToUser() {
        beginAndGetGame();
        setupSecurityContext(UUID.randomUUID());
        assertThatThrownBy(() -> gameService.restoreGameState())
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void restoreGameStateShouldThrowWhenNotAuthenticated() {
        assertThatThrownBy(() -> gameService.restoreGameState())
                .isInstanceOf(InvalidUserException.class);
    }
}