package com.tienda.inventory;

import com.tienda.inventory.dto.InventoryResponse;
import com.tienda.inventory.dto.MovementRequest;
import com.tienda.inventory.dto.MovementResponse;
import com.tienda.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{variantId}")
    public InventoryResponse getByVariant(@PathVariable UUID variantId) {
        return inventoryService.getByVariantId(variantId);
    }

    @GetMapping("/{variantId}/movements")
    public Page<MovementResponse> listMovements(
            @PathVariable UUID variantId,
            @ParameterObject Pageable pageable) {
        return inventoryService.listMovements(variantId, pageable);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse registerMovement(
            @Valid @RequestBody MovementRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return inventoryService.registerMovement(request, principal.getUser());
    }
}
