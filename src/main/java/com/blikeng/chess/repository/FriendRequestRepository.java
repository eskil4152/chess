package com.blikeng.chess.repository;

import com.blikeng.chess.entity.FriendRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, UUID> {
    boolean existsByFromUserAndToUser(UUID fromUser, UUID toUser);
}
