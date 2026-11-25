package com.tienda.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** En producción el sistema no arranca si quedaron las claves de ejemplo. */
@Slf4j
@Profile("prod")
@Component
public class ProductionGuard {

    private static final String DEFAULT_JWT = "cambia-esta-clave-en-produccion-minimo-32-caracteres-1234567890";
    private static final String DEFAULT_DB_PASSWORD = "change_me_local";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.orders.allow-delete:false}")
    private boolean allowDelete;

    @PostConstruct
    void check() {
        if (jwtSecret == null || jwtSecret.equals(DEFAULT_JWT) || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET no está definida o es la de ejemplo: usa una clave propia de 32 caracteres o más");
        }
        if (dbPassword == null || dbPassword.isBlank() || dbPassword.equals(DEFAULT_DB_PASSWORD)) {
            throw new IllegalStateException("POSTGRES_PASSWORD no está definida o es la de ejemplo");
        }
        if (allowDelete) {
            log.warn("ORDERS_ALLOW_DELETE está en true: en producción conviene ponerlo en false");
        }
    }
}
