package com.blikeng.chess.unit.security;

import com.blikeng.chess.security.ratelimit.RateLimitInterceptor;
import com.blikeng.chess.security.ratelimit.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock RateLimitingService rateLimitingService;

    RateLimitInterceptor interceptor;

    @BeforeEach
    void setup() {
        interceptor = new RateLimitInterceptor(rateLimitingService);
        ReflectionTestUtils.setField(interceptor, "maxLoginTokens", 5L);
        ReflectionTestUtils.setField(interceptor, "registerMaxTokens", 10L);
        ReflectionTestUtils.setField(interceptor, "otherMaxTokens", 60L);
    }

    @Test
    void shouldAllowRequestWhenNotRateLimited() throws Exception {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/someUser");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
    }

    @Test
    void shouldReturn429WhenRateLimited() throws Exception {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, null)).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void shouldUseLoginBucketForLoginPath() throws Exception {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("1.2.3.4");

        interceptor.preHandle(request, new MockHttpServletResponse(), null);

        verify(rateLimitingService).tryConsume(eq("login:1.2.3.4"), eq(5L), any(Duration.class));
    }

    @Test
    void shouldUseRegisterBucketForRegisterPath() throws Exception {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setRemoteAddr("1.2.3.4");

        interceptor.preHandle(request, new MockHttpServletResponse(), null);

        verify(rateLimitingService).tryConsume(eq("register:1.2.3.4"), eq(10L), any(Duration.class));
    }

    @Test
    void shouldFallbackToUnknownWhenIpIsNull() throws Exception {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games/active");
        request.setRemoteAddr(null);

        interceptor.preHandle(request, new MockHttpServletResponse(), null);

        verify(rateLimitingService).tryConsume(eq("others:unknown"), eq(60L), any(Duration.class));
    }

    @Test
    void shouldUseOtherBucketForUnknownPath() throws Exception {
        when(rateLimitingService.tryConsume(any(), any(), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games/active");
        request.setRemoteAddr("1.2.3.4");

        interceptor.preHandle(request, new MockHttpServletResponse(), null);

        verify(rateLimitingService).tryConsume(eq("others:1.2.3.4"), eq(60L), any(Duration.class));
    }
}