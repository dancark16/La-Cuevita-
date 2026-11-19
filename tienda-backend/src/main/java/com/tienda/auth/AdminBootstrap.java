package com.tienda.auth;

import com.tienda.users.Role;
import com.tienda.users.RoleRepository;
import com.tienda.users.User;
import com.tienda.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el administrador inicial en una instalación nueva.
 * Solo actúa si defines ADMIN_EMAIL y ADMIN_PASSWORD y ese correo aún no existe.
 * La contraseña no se vuelve a leer ni se cambia en los arranques siguientes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final int MIN_PASSWORD = 10;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:}")
    private String email;

    @Value("${app.bootstrap.admin-password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        if (password.length() < MIN_PASSWORD) {
            log.warn("ADMIN_PASSWORD debe tener al menos {} caracteres: no se creó el administrador", MIN_PASSWORD);
            return;
        }
        Role admin = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMIN"));
        userRepository.save(User.builder()
                .role(admin)
                .email(normalized)
                .passwordHash(passwordEncoder.encode(password))
                .enabled(true)
                .build());
        log.info("Administrador inicial creado: {}", normalized);
    }
}
