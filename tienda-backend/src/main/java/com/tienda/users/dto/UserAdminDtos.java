package com.tienda.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserAdminDtos {

    private UserAdminDtos() {
    }

    public record UserSummary(UUID id, String email, String role, boolean enabled, Instant createdAt) {
    }

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 10, max = 100, message = "La contraseña debe tener al menos 10 caracteres") String password,
            @Pattern(regexp = "ADMIN|CUSTOMER", message = "El rol debe ser ADMIN o CUSTOMER") String role
    ) {
    }

    public record UpdateUserRequest(
            @Pattern(regexp = "ADMIN|CUSTOMER", message = "El rol debe ser ADMIN o CUSTOMER") String role,
            Boolean enabled
    ) {
    }
}
