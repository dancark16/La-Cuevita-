package com.tienda.invoices;

import com.tienda.invoices.dto.InvoiceResponse;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrder().getId(),
                invoice.getStatus(),
                invoice.getInvoiceNumber(),
                invoice.getAccessKey(),
                invoice.getXmlUrl(),
                invoice.getPdfUrl(),
                invoice.getTaxAmount(),
                invoice.getTotal(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt());
    }
}
