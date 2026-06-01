package com.blikeng.chess.repository;

import com.blikeng.chess.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Page<UserEntity> findAllByOrderByBulletEloDesc(Pageable pageable);
    Page<UserEntity> findAllByOrderByBlitzEloDesc(Pageable pageable);
    Page<UserEntity> findAllByOrderByRapidEloDesc(Pageable pageable);
    Page<UserEntity> findAllByOrderByClassicalEloDesc(Pageable pageable);
}
