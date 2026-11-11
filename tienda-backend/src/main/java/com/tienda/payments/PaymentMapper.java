package com.tienda.payments;

import com.tienda.payments.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getProviderReference(),
                payment.getCreatedAt());
    }
}
