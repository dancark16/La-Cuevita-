package com.tienda.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita cuántas veces una misma IP puede llamar a las puertas de entrada (ingresar, registrarse,
 * recuperar contraseña) en un rato. Frena a quien prueba miles de combinaciones desde un solo lugar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/google",
            "/api/auth/forgot-password", "/api/auth/reset-password");

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration window;

    public AuthRateLimitFilter(
            @Value("${app.security.auth-ip-max:30}") int maxRequests,
            @Value("${app.security.auth-ip-window-minutes:10}") int windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PROTECTED.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Deque<Instant> recent = hits.computeIfAbsent(request.getRemoteAddr(), k -> new ArrayDeque<>());
        long retryAfter = 0;
        synchronized (recent) {
            Instant limit = Instant.now().minus(window);
            while (!recent.isEmpty() && recent.peekFirst().isBefore(limit)) {
                recent.pollFirst();
            }
            if (recent.size() >= maxRequests) {
                retryAfter = Math.max(1, Duration.between(Instant.now(), recent.peekFirst().plus(window)).toSeconds());
            } else {
                recent.addLast(Instant.now());
            }
        }
        if (retryAfter > 0) {
            long minutes = (retryAfter + 59) / 60;
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Demasiados intentos desde tu conexión. Espera "
                    + minutes + (minutes == 1 ? " minuto" : " minutos") + " y vuelve a intentarlo.\",\"details\":[]}").getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    void cleanup() {
        Instant limit = Instant.now().minus(window);
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                while (!entry.getValue().isEmpty() && entry.getValue().peekFirst().isBefore(limit)) {
                    entry.getValue().pollFirst();
                }
                return entry.getValue().isEmpty();
            }
        });
    }
}
