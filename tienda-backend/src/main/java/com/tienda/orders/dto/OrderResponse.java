package com.tienda.orders.dto;

import com.tienda.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        Long orderNumber,
        OrderStatus status,
        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,
        String customerIdNumber,
        AddressSummary address,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        boolean hasReceipt,
        Instant receiptUploadedAt,
        String adminNote,
        Instant paidAt,
        Instant shippedAt,
        Instant createdAt,
        Instant updatedAt
        ) {
}
