package com.blikeng.chess.security;

import java.util.UUID;

/** Authenticated user details extracted from a JWT and held in the security context. */
public record JwtPrincipal(UUID userId, String username, UserRole role) {}

