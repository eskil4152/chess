package com.blikeng.chess.repository;

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

    /** Finished games for a user in a time-control category; matches by enum-name prefix (e.g. "RAPID" → "RAPID_10_0"). */
    @Query("""
        SELECT game
        FROM GameEntity game
        WHERE
            (game.black.username = :username OR game.white.username = :username)
        AND
            game.timeControl LIKE CONCAT(:tcType, '%')
        AND
            game.status <> 'ONGOING'
    """)
    List<GameEntity> findFinishedByUsernameAndTcType(@Param("username") String username, @Param("tcType") String tcType);
}