package com.blikeng.chess.controller;

import com.blikeng.chess.bot.BotDefinition;
import com.blikeng.chess.bot.BotDifficulty;
import com.blikeng.chess.bot.BotService;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.service.AuthService;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Endpoints for playing against bots (base path {@code /api/bot}).
 *
 * <p>{@code POST /{difficulty}} starts a game between the current user and a bot of the
 * given {@link BotDifficulty}.
 */
@Controller
@RequestMapping("/api/bot")
public class BotController {
    private final BotService botService;
    private final ActiveGameStore activeGameStore;
    private final GameCreationService gameCreationService;
    private final AuthService authService;

    public BotController(BotService botService, ActiveGameStore activeGameStore, GameCreationService gameCreationService, AuthService authService) {
        this.botService = botService;
        this.activeGameStore = activeGameStore;
        this.gameCreationService = gameCreationService;
        this.authService = authService;
    }

    @PostMapping("/{difficulty}")
    public ResponseEntity<Void> playVsBot(@PathVariable String difficulty) {
        JwtPrincipal principal = JwtService.getCurrentUser();
        if (principal == null || principal.userId() == null) throw new InvalidUserException();

        BotDifficulty botDifficulty = BotDifficulty.valueOf(difficulty.toUpperCase());

        if (activeGameStore.isInGame(principal.userId())) throw new ExistingGameException();

        UserEntity player = authService.findUserById(principal.userId()).orElseThrow(InvalidUserException::new);
        BotDefinition bot = botService.getBot(botDifficulty);

        gameCreationService.beginBotGame(player, bot);

        return ResponseEntity.ok().build();
    }
}
