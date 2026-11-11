package com.tienda.auth;

import com.tienda.auth.dto.AccessTokenResponse;
import com.tienda.auth.dto.AuthResponse;
import com.tienda.auth.dto.GoogleLoginRequest;
import com.tienda.auth.dto.LoginRequest;
import com.tienda.auth.dto.RefreshRequest;
import com.tienda.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory cookieFactory;

    @PostMapping("/register")
    public ResponseEntity<AccessTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withRefreshCookie(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AccessTokenResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return withRefreshCookie(authService.loginWithGoogle(request.idToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(new RefreshRequest(requireToken(refreshToken))));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(new RefreshRequest(refreshToken));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    private ResponseEntity<AccessTokenResponse> withRefreshCookie(AuthResponse auth) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(auth.refreshToken()).toString())
                .body(new AccessTokenResponse(auth.accessToken(), auth.tokenType(), auth.expiresIn()));
    }

    private String requireToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("No hay una sesión activa");
        }
        return refreshToken;
    }
}
