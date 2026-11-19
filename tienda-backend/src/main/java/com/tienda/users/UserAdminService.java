package com.tienda.users;

import com.tienda.auth.EmailAlreadyInUseException;
import com.tienda.auth.PasswordResetService;
import com.tienda.auth.RefreshTokenRepository;
import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.users.dto.UserAdminDtos.CreateUserRequest;
import com.tienda.users.dto.UserAdminDtos.UpdateUserRequest;
import com.tienda.users.dto.UserAdminDtos.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Administración de cuentas desde el panel: crear administradores, cambiar rol y activar o desactivar. */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final String ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    @Transactional(readOnly = true)
    public Page<UserSummary> search(String q, String role, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : q.trim();
        String roleName = role == null || role.isBlank() ? null : role;
        return userRepository.search(term, roleName, pageable).map(this::toSummary);
    }

    @Transactional
    public UserSummary create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }
        String roleName = request.role() == null ? ADMIN : request.role();
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + roleName));
        User user = userRepository.save(User.builder()
                .role(role)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .build());
        return toSummary(user);
    }

    @Transactional
    public UserSummary update(UUID id, UpdateUserRequest request, UUID actingUserId) {
        User user = get(id);
        boolean changesRole = request.role() != null && !request.role().equals(user.getRole().getName());
        boolean disables = Boolean.FALSE.equals(request.enabled()) && user.isEnabled();

        if (id.equals(actingUserId) && (changesRole || disables)) {
            throw new IllegalArgumentException("No puedes cambiar tu propio rol ni desactivar tu propia cuenta");
        }
        boolean losesAdminPower = user.getRole().getName().equals(ADMIN) && user.isEnabled()
                && (changesRole || disables);
        if (losesAdminPower && userRepository.countByRoleNameAndEnabledTrue(ADMIN) <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo");
        }

        if (changesRole) {
            Role role = roleRepository.findByName(request.role())
                    .orElseThrow(() -> new IllegalStateException("No existe el rol " + request.role()));
            user.setRole(role);
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
            if (disables) {
                refreshTokenRepository.revokeAllByUserId(user.getId());
            }
        }
        return toSummary(userRepository.save(user));
    }

    /** Envía a la persona un enlace para que cree su propia contraseña. */
    @Transactional
    public void sendResetLink(UUID id) {
        passwordResetService.requestReset(get(id).getEmail());
    }

    private User get(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getRole().getName(), u.isEnabled(), u.getCreatedAt());
    }
}
