package com.tienda.invoices.dto;

import com.tienda.integrations.billing.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID orderId,
        InvoiceStatus status,
        String invoiceNumber,
        String accessKey,
        String xmlUrl,
        String pdfUrl,
        BigDecimal taxAmount,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
        ) {

}
