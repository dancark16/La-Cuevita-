package com.tienda.payments;

import com.tienda.payments.dto.PaymentRequest;
import com.tienda.payments.dto.PaymentResponse;
import com.tienda.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return paymentService.pay(orderId, request, principal.getUser());
    }

    @GetMapping("/payments")
    public List<PaymentResponse> findByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return paymentService.findByOrder(orderId, principal.getUser());
    }
}
