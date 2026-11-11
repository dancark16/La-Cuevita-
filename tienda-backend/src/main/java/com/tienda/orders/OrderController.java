package com.tienda.orders;

import com.tienda.orders.dto.CheckoutRequest;
import com.tienda.orders.dto.OrderResponse;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.checkout(request, principal.getUser());
    }

    @GetMapping
    public Page<OrderResponse> findMine(@AuthenticationPrincipal CustomUserDetails principal,
            @ParameterObject Pageable pageable) {
        return orderService.findMine(principal.getUser(), pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.findById(id, principal.getUser());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.cancel(id, principal.getUser());
    }
}
