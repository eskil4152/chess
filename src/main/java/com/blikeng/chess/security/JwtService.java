package com.blikeng.chess.security;

import com.blikeng.chess.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates the HS512-signed JWTs used for authentication.
 *
 * <p>Token lifetime follows the login's remember-me choice: 1 day normally, 30 days when
 * remembered, so it stays in sync with the auth cookie's max-age.
 *
 * <p>The signing secret comes from {@code app.jwt.secret} and must be at least 64 bytes.
 * {@link #validateToken} returns the {@link JwtPrincipal} or {@code null} if invalid;
 * {@link #getCurrentUser} reads the principal from the current SecurityContext.
 */
@Service
public class JwtService {

    private static final long DEFAULT_VALIDITY_MS = 24L * 60 * 60 * 1000;
    private static final long REMEMBER_ME_VALIDITY_MS = 30L * 24 * 60 * 60 * 1000;

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret}")
    private String secret;

    @PostConstruct
    public void validateSecret() {
        if (secret.getBytes().length < 64) {
            throw new IllegalStateException("Secret must be at least 64 bytes long");
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserEntity user, boolean rememberMe) {
        long validityMs = rememberMe ? REMEMBER_ME_VALIDITY_MS : DEFAULT_VALIDITY_MS;
        return Jwts
                .builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validityMs))
                .signWith(key(), Jwts.SIG.HS512)
                .compact();
    }

    public JwtPrincipal validateToken(String token) {
        try {
            Claims claims = Jwts
                    .parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            UserRole role = UserRole.valueOf(claims.get("role", String.class));

            return new JwtPrincipal(userId, username, role);
        } catch (Exception e) {
            logger.error("Invalid token: {}", e.getMessage());
            return null;
        }
    }

    public static JwtPrincipal getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        return auth.getPrincipal() instanceof JwtPrincipal p ? p : null;
    }
}
