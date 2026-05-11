package com.blikeng.chess.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.blikeng.chess.entity.FriendEntity;
import com.blikeng.chess.entity.FriendId;

public interface FriendRepository extends JpaRepository<FriendEntity, FriendId>{
}
