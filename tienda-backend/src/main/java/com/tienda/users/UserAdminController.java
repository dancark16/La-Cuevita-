package com.tienda.users;

import com.tienda.security.CustomUserDetails;
import com.tienda.users.dto.UserAdminDtos.CreateUserRequest;
import com.tienda.users.dto.UserAdminDtos.UpdateUserRequest;
import com.tienda.users.dto.UserAdminDtos.UserSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService service;

    @GetMapping
    public Page<UserSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.search(q, role, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}")
    public UserSummary update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return service.update(id, request, principal.getUser().getId());
    }

    /** Envía un enlace para que la persona cree una contraseña nueva. */
    @PostMapping("/{id}/reset-link")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendResetLink(@PathVariable UUID id) {
        service.sendResetLink(id);
    }
}
