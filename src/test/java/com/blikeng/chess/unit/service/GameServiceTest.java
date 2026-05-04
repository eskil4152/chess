package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.errorTypes.GameNotFoundException;
import com.blikeng.chess.exception.errorTypes.InvalidMoveException;
import com.blikeng.chess.exception.errorTypes.InvalidUUIDException;
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
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

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

    private void stubSave() {
        when(gameRepository.save(any())).thenReturn(savedEntity);
    }

    private Game beginAndGetGame() {
        stubSave();
        gameService.beginGame(white, black);
        return gameService.getActiveGame(white.getId()).orElseThrow();
    }


    // --- Begin Game ---
    @Test
    void beginGameShouldSaveEntityAndPublishEvent() {
        stubSave();
        gameService.beginGame(white, black);

        verify(gameRepository).save(any());
        ArgumentCaptor<MatchStartedEvent> captor = ArgumentCaptor.forClass(MatchStartedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().whiteUsername()).isEqualTo("white");
        assertThat(captor.getValue().blackUsername()).isEqualTo("black");
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

    @Test
    void isInGameShouldReturnFalseWhenPlayerNotInExistingGame() {
        beginAndGetGame();
        assertThat(gameService.isInGame(UUID.randomUUID())).isFalse();
    }

    // --- Get Active Game ---
    @Test
    void getActiveGameShouldReturnGameForBlackPlayer() {
        beginAndGetGame();
        assertThat(gameService.getActiveGame(black.getId())).isPresent();
    }

    @Test
    void getActiveGameShouldReturnEmptyWhenNoGamesExist() {
        assertThat(gameService.getActiveGame(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getActiveGameShouldReturnEmptyWhenPlayerNotInExistingGame() {
        beginAndGetGame();
        assertThat(gameService.getActiveGame(UUID.randomUUID())).isEmpty();
    }


    // --- Is User Turn ---
    @Test
    void makeMoveShouldAllowBlackMoveWhenItIsBlacksTurn() {
        Game game = beginAndGetGame();
        gameService.makeMove(white.getId(), new WsMoveDTO(game.getId().toString(), "e2e4"));
        gameService.makeMove(black.getId(), new WsMoveDTO(game.getId().toString(), "e7e5"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(3)).publishEvent(captor.capture());
        assertThat(captor.getAllValues().stream().filter(e -> e instanceof MoveMadeEvent).count()).isEqualTo(2);
    }

    // --- Make Move ---
    @Test
    void makeMoveShouldThrowWhenGameNotFound() {
        WsMoveDTO dto = new WsMoveDTO(UUID.randomUUID().toString(), "e2e4");
        assertThatThrownBy(() -> gameService.makeMove(white.getId(), dto))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void makeMoveShouldThrowOnInvalidUUIDFormat() {
        WsMoveDTO dto = new WsMoveDTO("not-valid-uuid", "e2e4");
        assertThatThrownBy(() -> gameService.makeMove(white.getId(), dto))
                .isInstanceOf(InvalidUUIDException.class);
    }

    @Test
    void makeMoveShouldThrowWhenMoveTooShort() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2");
        assertThatThrownBy(() -> gameService.makeMove(white.getId(), dto))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void makeMoveShouldReturnEarlyWhenNotPlayersTurn() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e7e5");
        gameService.makeMove(black.getId(), dto);
        verify(eventPublisher, times(1)).publishEvent(any(MatchStartedEvent.class));
    }

    @Test
    void makeMoveShouldPublishMoveMadeEventForValidMove() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2e4");
        gameService.makeMove(white.getId(), dto);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof MoveMadeEvent);
    }

    @Test
    void makeMoveShouldNotPublishEventForInvalidMove() {
        Game game = beginAndGetGame();
        WsMoveDTO dto = new WsMoveDTO(game.getId().toString(), "e2d3");
        gameService.makeMove(white.getId(), dto);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(1)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).noneMatch(e -> e instanceof MoveMadeEvent);
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

        gameService.makeMove(white.getId(), new WsMoveDTO(game.getId().toString(), "c7c8q"));

        assertThat(game.getBoard().getPiece(7, 2)).isInstanceOf(Queen.class);
    }


    // --- Game End ---
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

        gameService.makeMove(white.getId(), new WsMoveDTO(game.getId().toString(), "g5g7"));

        verify(gameRepository).findById(game.getId());
        verify(gameRepository, atLeast(2)).save(any());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof MatchEndedEvent);
        assertThat(gameService.isInGame(white.getId())).isFalse();
    }


    // --- Session Connected ---
    @Test
    void onSessionConnectedShouldSendGameStateWhenPlayerIsInGame() {
        beginAndGetGame();
        WebSocketSession session = mock(WebSocketSession.class);
        gameService.onSessionConnected(white.getId(), session);
        verify(notificationService).sendToSession(eq(session), any(String.class));
    }

    @Test
    void onSessionConnectedShouldSendGameStateForBlackPlayer() {
        beginAndGetGame();
        WebSocketSession session = mock(WebSocketSession.class);
        gameService.onSessionConnected(black.getId(), session);
        verify(notificationService).sendToSession(eq(session), any(String.class));
    }

    @Test
    void onSessionConnectedShouldDoNothingWhenPlayerNotInGame() {
        WebSocketSession session = mock(WebSocketSession.class);
        gameService.onSessionConnected(UUID.randomUUID(), session);
        verifyNoInteractions(notificationService);
    }

    @Test
    void onSessionConnectedShouldDoNothingWhenPlayerNotInExistingGame() {
        beginAndGetGame();
        WebSocketSession session = mock(WebSocketSession.class);
        gameService.onSessionConnected(UUID.randomUUID(), session);
        verifyNoInteractions(notificationService);
    }

    @Test
    void onSessionConnectedShouldNotSendOnJsonException() throws Exception {
        beginAndGetGame();
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail") {});
        ReflectionTestUtils.setField(gameService, "objectMapper", failingMapper);

        WebSocketSession session = mock(WebSocketSession.class);
        gameService.onSessionConnected(white.getId(), session);
        verifyNoInteractions(notificationService);
    }
}