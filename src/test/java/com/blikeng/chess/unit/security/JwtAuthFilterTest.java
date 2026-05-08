package com.blikeng.chess.unit.security;

import com.blikeng.chess.security.JwtAuthFilter;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @InjectMocks JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotSetAuthenticationWhenNoCookies() throws Exception {
        jwtAuthFilter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotSetAuthenticationWhenOnlyOtherCookiePresent() throws Exception {
        request.setCookies(new Cookie("SESSION", "abc"));
        jwtAuthFilter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipValidationWhenBlankCookie() throws Exception {
        request.setCookies(new Cookie("AUTH", "   "));
        jwtAuthFilter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "user", UserRole.USER);
        when(jwtService.validateToken("valid-token")).thenReturn(principal);
        request.setCookies(new Cookie("AUTH", "valid-token"));

        jwtAuthFilter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(principal);
    }

    @Test
    void shouldNotSetAuthenticationWhenInvalidToken() throws Exception {
        when(jwtService.validateToken("bad-token")).thenReturn(null);
        request.setCookies(new Cookie("AUTH", "bad-token"));

        jwtAuthFilter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipValidationWhenAuthAlreadyExists() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), "user", UserRole.USER);
        var existingAuth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        request.setCookies(new Cookie("AUTH", "some-token"));
        jwtAuthFilter.doFilterInternal(request, response, chain);

        verifyNoInteractions(jwtService);
        verify(chain).doFilter(request, response);
    }
}
