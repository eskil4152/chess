package com.blikeng.chess.security;

import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.ErrorTypes.InvalidUserException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

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

    public String generateToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();
    }

    public JwtPrincipal validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);

            return new JwtPrincipal(userId, username);
        } catch (Exception e) {
            logger.error("Invalid token: {}", e.getMessage());
            return null;
        }
    }

    public static JwtPrincipal getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        return (JwtPrincipal) auth.getPrincipal();
    }
}
