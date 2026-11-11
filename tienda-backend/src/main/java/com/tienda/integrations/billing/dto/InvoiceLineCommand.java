package com.tienda.integrations.billing.dto;

import java.math.BigDecimal;

public record InvoiceLineCommand(
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal subtotal
        ) {

}
