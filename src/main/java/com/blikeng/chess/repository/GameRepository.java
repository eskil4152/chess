package com.blikeng.chess.repository;

import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {
    @Query("""
        select g
        from GameEntity g
        where
            g.black.username = :username
        or
            g.white.username = :username
    """)
    List<GameEntity> findAllByUsername(@Param("username") String username);
}
