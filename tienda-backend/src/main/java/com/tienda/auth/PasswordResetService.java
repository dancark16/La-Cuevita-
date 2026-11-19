package com.tienda.auth;

import com.tienda.common.mail.BrandedMailService;
import com.tienda.users.User;
import com.tienda.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperación de contraseña por correo.
 * El enlace dura 1 hora y sirve una sola vez; en la base solo se guarda su hash.
 * Responde igual exista o no el correo, para no revelar quién tiene cuenta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration VALID_FOR = Duration.ofHours(1);
    private static final Duration MIN_INTERVAL = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrandedMailService mail;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /** Pide el enlace. Si el correo no existe, no hace nada (y no lo dice). */
    @Transactional
    public void requestReset(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        userRepository.findByEmail(normalized).filter(User::isEnabled).ifPresent(user -> {
            if (tokenRepository.existsByUserIdAndCreatedAtAfter(user.getId(), Instant.now().minus(MIN_INTERVAL))) {
                return; // ya se envió uno hace menos de un minuto
            }
            tokenRepository.deleteAllByUserId(user.getId());
            String raw = newToken();
            tokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(hash(raw))
                    .expiresAt(Instant.now().plus(VALID_FOR))
                    .build());
            String link = frontendUrl.replaceAll("/+$", "") + "/restablecer?token=" + raw;
            afterCommit(() -> sendAsync(user.getEmail(), link));
        });
    }

    /** Cambia la contraseña con un enlace válido y cierra todas las sesiones abiertas. */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken == null ? "" : rawToken))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido o ya venció. Pide uno nuevo."));
        User user = userRepository.findById(token.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido o ya venció. Pide uno nuevo."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Async
    public void sendAsync(String to, String link) {
        try {
            if (!mail.isConfigured()) {
                // Solo en desarrollo, sin correo configurado: así se puede probar el flujo completo.
                log.warn("Correo sin configurar. Enlace de recuperación para {}: {}", to, link);
                return;
            }
            mail.send(to, "Recupera tu contraseña de La Cuevita", "Crea una contraseña nueva",
                    "Recibimos una solicitud para cambiar la contraseña de tu cuenta. El enlace funciona una sola vez y vence en 1 hora.",
                    "Crear contraseña nueva", link,
                    "Si no fuiste tú, ignora este correo: tu contraseña no cambiará.");
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el correo de recuperación", e);
        }
    }

    private static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
