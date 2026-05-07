package com.blikeng.chess.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String username, UserRole role) {}

