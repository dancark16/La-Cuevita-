package com.tienda.payments.dto;

import com.tienda.payments.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String method,
        PaymentStatus status,
        BigDecimal amount,
        String providerReference,
        Instant createdAt
        ) {

}
