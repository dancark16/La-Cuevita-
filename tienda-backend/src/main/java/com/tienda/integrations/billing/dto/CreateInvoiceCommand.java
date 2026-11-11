package com.tienda.integrations.billing.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateInvoiceCommand(
        String idempotencyKey,
        String customerIdType,
        String customerIdNumber,
        String customerName,
        String customerEmail,
        String customerPhone,
        String customerAddress,
        List<InvoiceLineCommand> lines,
        String paymentMethod,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total
        ) {

}
