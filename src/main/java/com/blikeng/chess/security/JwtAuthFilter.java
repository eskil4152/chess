package com.blikeng.chess.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Per-request filter that authenticates via the {@code AUTH} cookie: validates the JWT
 * (rejecting blacklisted tokens) and, if valid, sets the {@link JwtPrincipal} on the Spring
 * SecurityContext. Always continues the filter chain. Unauthenticated requests are left
 * for {@code SecurityConfig}'s authorization rules to reject.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final Blacklist blacklist;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtService jwtService, Blacklist blacklist) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Override
    public void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null &&
            !token.isBlank() &&
            SecurityContextHolder.getContext().getAuthentication() == null
        ) {
            JwtPrincipal jwtPrincipal = jwtService.validateToken(token);

            if (blacklist.contains(token)){
                chain.doFilter(request, response);

                return;
            }

            if (jwtPrincipal != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(jwtPrincipal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Invalid JWT token in request to {}", request.getRequestURI());
            }
        }

        chain.doFilter(request, response);
    }
}