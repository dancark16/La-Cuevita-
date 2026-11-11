package com.tienda.integrations.billing.mock;

import com.tienda.integrations.billing.BillingIntegrationService;
import com.tienda.integrations.billing.InvoiceStatus;
import com.tienda.integrations.billing.dto.CreateInvoiceCommand;
import com.tienda.integrations.billing.dto.InvoiceResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MockBillingIntegrationService implements BillingIntegrationService {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, InvoiceResult> store = new ConcurrentHashMap<>();

    @Override
    public InvoiceResult createInvoice(CreateInvoiceCommand command) {
        InvoiceResult existing = store.get(command.idempotencyKey());
        if (existing != null) {
            return existing;
        }

        String invoiceNumber = "001-001-%09d".formatted(sequence.getAndIncrement());
        String accessKey = UUID.randomUUID().toString().replace("-", "");

        InvoiceResult result = new InvoiceResult(
                command.idempotencyKey(),
                InvoiceStatus.AUTHORIZED,
                invoiceNumber,
                accessKey,
                "https://mock-billing.local/xml/" + command.idempotencyKey() + ".xml",
                "https://mock-billing.local/pdf/" + command.idempotencyKey() + ".pdf",
                null);

        store.put(command.idempotencyKey(), result);
        return result;
    }

    @Override
    public InvoiceResult getInvoiceStatus(String externalReference) {
        return getInvoice(externalReference);
    }

    @Override
    public InvoiceResult getInvoice(String externalReference) {
        InvoiceResult result = store.get(externalReference);
        if (result == null) {
            throw new IllegalStateException("Factura no encontrada en el proveedor: " + externalReference);
        }
        return result;
    }

    @Override
    public void cancelInvoice(String externalReference, String reason) {
        InvoiceResult existing = getInvoice(externalReference);
        store.put(externalReference, new InvoiceResult(
                existing.externalReference(),
                InvoiceStatus.CANCELLED,
                existing.invoiceNumber(),
                existing.accessKey(),
                existing.xmlUrl(),
                existing.pdfUrl(),
                reason));
    }

    @Override
    public byte[] downloadXml(String externalReference) {
        InvoiceResult result = getInvoice(externalReference);
        String xml = "<factura><claveAcceso>%s</claveAcceso><numero>%s</numero></factura>"
                .formatted(result.accessKey(), result.invoiceNumber());
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] downloadPdf(String externalReference) {
        InvoiceResult result = getInvoice(externalReference);
        String placeholder = "RIDE MOCK - Factura " + result.invoiceNumber();
        return placeholder.getBytes(StandardCharsets.UTF_8);
    }
}
