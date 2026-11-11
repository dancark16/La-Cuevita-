package com.tienda.integrations.billing;

import com.tienda.integrations.billing.dto.CreateInvoiceCommand;
import com.tienda.integrations.billing.dto.InvoiceResult;

public interface BillingIntegrationService {

    InvoiceResult createInvoice(CreateInvoiceCommand command);

    InvoiceResult getInvoiceStatus(String externalReference);

    InvoiceResult getInvoice(String externalReference);

    void cancelInvoice(String externalReference, String reason);

    byte[] downloadXml(String externalReference);

    byte[] downloadPdf(String externalReference);
}
