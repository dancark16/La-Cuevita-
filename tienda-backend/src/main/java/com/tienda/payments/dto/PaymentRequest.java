package com.tienda.payments.dto;

import com.tienda.payments.PaymentStatus;
import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank
        String method,
        PaymentStatus simulateStatus
        ) {

}
