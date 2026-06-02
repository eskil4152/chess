package com.blikeng.chess.repository;

import com.blikeng.chess.entity.FriendRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, UUID> {
    boolean existsByFromUser_IdAndToUser(UUID fromUser, UUID toUser);

    void deleteByFromUser_IdAndToUser(UUID fromUser, UUID toUser);

    List<FriendRequestEntity> findAllByToUser(UUID toUser);
}
