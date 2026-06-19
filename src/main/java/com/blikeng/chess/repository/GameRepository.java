package com.blikeng.chess.repository;

import com.blikeng.chess.dto.GameStatRow;
import com.blikeng.chess.entity.GameEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Persisted game (history) queries. */
@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {
    @Query("""
        select g
        from GameEntity g
        where
            g.black.username = :username
        or
            g.white.username = :username
        order by g.createdAt desc
    """)
    Page<GameEntity> findByUsernameOrderedByTimestampDesc(@Param("username") String username, Pageable pageable);

    /**
     * Lightweight stat projections for a user's finished games in a time-control category;
     * matches by enum-name prefix (e.g. "RAPID" → "RAPID_10_0"). Filters on the indexed
     * white/black id columns (no join to users) and selects only the fields needed to
     * aggregate stats, avoiding full entity loads and the heavy moves column.
     */
    @Query("""
        SELECT new com.blikeng.chess.dto.GameStatRow(game.white.id, game.status, game.endedBy)
        FROM GameEntity game
        WHERE
            (game.white.id = :userId OR game.black.id = :userId)
        AND
            game.timeControl LIKE CONCAT(:tcType, '%')
        AND
            game.status <> 'ONGOING'
    """)
    List<GameStatRow> findFinishedStatsByUserAndTcType(@Param("userId") UUID userId, @Param("tcType") String tcType);
}