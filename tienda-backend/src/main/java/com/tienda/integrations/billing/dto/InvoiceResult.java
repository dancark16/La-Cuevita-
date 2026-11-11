package com.tienda.integrations.billing.dto;

import com.tienda.integrations.billing.InvoiceStatus;

public record InvoiceResult(
        String externalReference,
        InvoiceStatus status,
        String invoiceNumber,
        String accessKey,
        String xmlUrl,
        String pdfUrl,
        String rejectionReason
        ) {

}
