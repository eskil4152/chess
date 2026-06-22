package com.blikeng.chess.service;

import com.blikeng.chess.dto.TimeControlDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameCreationService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Elo-based matchmaking queue backed by Redis sorted sets.
 *
 * <p>A queuing player is paired with the closest-rated waiting player on the same
 * {@link TimeControl} within ±200 Elo via an atomic Lua script. If none qualifies
 * they wait in the queue.
 */
@Service
public class MatchmakingService {
    private final AuthService authService;
    private final ActiveGameStore activeGameStore;
    private final GameCreationService gameCreationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<String> script;

    public MatchmakingService(
        AuthService authService,
        ActiveGameStore activeGameStore,
        GameCreationService gameCreationService,
        RedisTemplate<String, String> redisTemplate
    ){
        this.authService = authService;
        this.activeGameStore = activeGameStore;
        this.gameCreationService = gameCreationService;
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setLocation(new ClassPathResource("scripts/matchmaking.lua"));
    }

    public void queuePlayer(TimeControlDTO timeControlDTO) {
        JwtPrincipal jwtPrincipal = JwtService.getCurrentUser();
        if (jwtPrincipal == null) throw new InvalidUserException();

        UUID userId = jwtPrincipal.userId();
        UserEntity user = authService.findUserById(userId).orElseThrow(InvalidUserException::new);

        if (activeGameStore.isInGame(userId)) throw new ExistingGameException();

        TimeControl requestedTc = timeControlDTO.resolved();
        String matchedUserId = redisTemplate.execute(
            script,
            List.of("queue:" + requestedTc.name()),
            userId.toString(),
            String.valueOf(user.getElo(requestedTc.type()))
        );

        if (matchedUserId != null){
            UserEntity matched = authService.findUserById(UUID.fromString(matchedUserId))
                .orElseThrow(InvalidUserException::new);
            gameCreationService.beginGame(matched, user, requestedTc);
        } else {
            redisTemplate.opsForValue().set("queue:tc:" + userId, requestedTc.name());
        }
    }

    public void dequeuePlayer() {
        JwtPrincipal jwtPrincipal = JwtService.getCurrentUser();
        if (jwtPrincipal == null) throw new InvalidUserException();

        UUID userId = jwtPrincipal.userId();

        String tc = redisTemplate.opsForValue().get("queue:tc:" + userId);
        if (tc != null) {
            redisTemplate.opsForZSet().remove("queue:" + tc, userId.toString());
            redisTemplate.delete("queue:tc:" + userId);
        }
    }

    public void dequeuePlayer(UUID userId) {
        String tc = redisTemplate.opsForValue().get("queue:tc:" + userId);
        if (tc != null) {
            redisTemplate.opsForZSet().remove("queue:" + tc, userId.toString());
            redisTemplate.delete("queue:tc:" + userId);
        }
    }
}
