package com.tienda.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protege las cuentas de adivinar la contraseña: tras varios intentos fallidos seguidos,
 * esa cuenta queda bloqueada un rato, aunque el siguiente intento sea correcto.
 * Se guarda en memoria (se reinicia con el backend), que basta para un solo servidor.
 */
@Service
public class LoginAttemptService {

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    private final int maxFailures;
    private final Duration window;

    public LoginAttemptService(
            @Value("${app.security.login-max-failures:5}") int maxFailures,
            @Value("${app.security.login-window-minutes:15}") int windowMinutes) {
        this.maxFailures = maxFailures;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /** Lanza TooManyAttemptsException si esa cuenta está bloqueada. */
    public void checkAllowed(String key) {
        Deque<Instant> recent = failures.get(key);
        if (recent == null) {
            return;
        }
        synchronized (recent) {
            prune(recent);
            if (recent.size() >= maxFailures) {
                long wait = Duration.between(Instant.now(), recent.peekFirst().plus(window)).toSeconds();
                throw new TooManyAttemptsException(wait);
            }
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (recent) {
            prune(recent);
            recent.addLast(Instant.now());
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> recent) {
        Instant limit = Instant.now().minus(window);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(limit)) {
            recent.pollFirst();
        }
    }

    /** Limpia lo viejo para que el mapa no crezca sin fin. */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    void cleanup() {
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue());
                return entry.getValue().isEmpty();
            }
        });
    }
}
