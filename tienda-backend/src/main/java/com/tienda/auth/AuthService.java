package com.tienda.auth;

import com.tienda.auth.dto.AuthResponse;
import com.tienda.auth.dto.LoginRequest;
import com.tienda.auth.dto.RefreshRequest;
import com.tienda.auth.dto.RegisterRequest;
import com.tienda.security.JwtService;
import com.tienda.users.Role;
import com.tienda.users.RoleRepository;
import com.tienda.users.User;
import com.tienda.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Rol por defecto no configurado: " + DEFAULT_ROLE));

        User user = User.builder()
                .role(role)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .build();

        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        String email = googleTokenVerifier.verify(idToken).email();

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByName(DEFAULT_ROLE)
                    .orElseThrow(() -> new IllegalStateException("Rol por defecto no configurado: " + DEFAULT_ROLE));
            return userRepository.save(User.builder()
                    .role(role)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .enabled(true)
                    .build());
        });

        if (!user.isEnabled()) {
            throw new InvalidGoogleTokenException("La cuenta está deshabilitada");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenValue = request.refreshToken();

        if (!jwtService.isTokenType(tokenValue, "refresh")) {
            throw new InvalidRefreshTokenException("El token no es un refresh token válido");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token no reconocido"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expirado o revocado");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().getName());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(jwtService.getRefreshTokenExpirationInstant())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds());
    }
}
