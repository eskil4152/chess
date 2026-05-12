package com.blikeng.chess.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.FriendId;

public interface FriendRepository extends JpaRepository<FriendEntity, FriendId>{

    @Query("""
        SELECT f
        FROM FriendEntity f
        JOIN FETCH f.userA
        JOIN FETCH f.userB
        WHERE f.userA.id = :userId OR f.userB.id = :userId
    """)
    public List<FriendEntity> findFriendsForUser(@Param("userId") UUID userId);
}
