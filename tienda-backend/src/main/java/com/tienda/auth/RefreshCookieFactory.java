package com.tienda.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;
    private final Duration maxAge;

    public RefreshCookieFactory(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.jwt.refresh-token-expiration-days}") long refreshDays) {
        this.secure = secure;
        this.maxAge = Duration.ofDays(refreshDays);
    }

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, maxAge);
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration age) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(age)
                .build();
    }
}
