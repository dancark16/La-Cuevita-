package com.tienda.invoices;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.customers.Customer;
import com.tienda.integrations.billing.BillingIntegrationService;
import com.tienda.integrations.billing.InvoiceStatus;
import com.tienda.integrations.billing.dto.CreateInvoiceCommand;
import com.tienda.integrations.billing.dto.InvoiceLineCommand;
import com.tienda.integrations.billing.dto.InvoiceResult;
import com.tienda.orders.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BillingIntegrationService billingIntegrationService;

    @Value("${app.billing.tax-rate}")
    private BigDecimal taxRate;

    @Transactional
    public Invoice requestInvoice(Order order) {
        String idempotencyKey = order.getId().toString();

        return invoiceRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createAndProcessInvoice(order, idempotencyKey));
    }

    @Transactional
    public Invoice retry(Order order) {
        String idempotencyKey = order.getId().toString();
        Invoice invoice = invoiceRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una factura previa para este pedido"));

        if (invoice.getStatus() == InvoiceStatus.AUTHORIZED) {
            return invoice;
        }

        CreateInvoiceCommand command = buildCommand(order, idempotencyKey);
        return attemptBilling(invoice, command);
    }

    @Transactional(readOnly = true)
    public Invoice getByOrder(Order order) {
        return invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aún no se ha generado una factura para este pedido"));
    }

    @Transactional(readOnly = true)
    public byte[] downloadXml(Order order) {
        Invoice invoice = getAuthorizedInvoiceOrThrow(order);
        return billingIntegrationService.downloadXml(invoice.getExternalReference());
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(Order order) {
        Invoice invoice = getAuthorizedInvoiceOrThrow(order);
        return billingIntegrationService.downloadPdf(invoice.getExternalReference());
    }

    private Invoice createAndProcessInvoice(Order order, String idempotencyKey) {
        CreateInvoiceCommand command = buildCommand(order, idempotencyKey);

        Invoice invoice = Invoice.builder()
                .order(order)
                .status(InvoiceStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .taxAmount(command.taxAmount())
                .total(command.total())
                .build();

        buildAndAttachItems(invoice, command);
        invoice = invoiceRepository.save(invoice);

        return attemptBilling(invoice, command);
    }

    private Invoice attemptBilling(Invoice invoice, CreateInvoiceCommand command) {
        try {
            InvoiceResult result = billingIntegrationService.createInvoice(command);
            applyResult(invoice, result);
        } catch (RuntimeException ex) {
            invoice.setStatus(InvoiceStatus.FAILED);
        }
        return invoiceRepository.save(invoice);
    }

    private void applyResult(Invoice invoice, InvoiceResult result) {
        invoice.setStatus(result.status());
        invoice.setInvoiceNumber(result.invoiceNumber());
        invoice.setAccessKey(result.accessKey());
        invoice.setXmlUrl(result.xmlUrl());
        invoice.setPdfUrl(result.pdfUrl());
        invoice.setExternalReference(result.externalReference());
    }

    private void buildAndAttachItems(Invoice invoice, CreateInvoiceCommand command) {
        List<InvoiceItem> items = command.lines().stream()
                .map(line -> InvoiceItem.builder()
                .invoice(invoice)
                .description(line.description())
                .quantity(line.quantity())
                .unitPrice(line.unitPrice())
                .taxRate(line.taxRate())
                .subtotal(line.subtotal())
                .build())
                .toList();
        invoice.getItems().addAll(items);
    }

    private CreateInvoiceCommand buildCommand(Order order, String idempotencyKey) {
        Customer customer = order.getCustomer();
        BigDecimal total = order.getTotal();
        BigDecimal divisor = BigDecimal.ONE.add(taxRate);
        BigDecimal baseImponible = total.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = total.subtract(baseImponible);

        List<InvoiceLineCommand> lines = order.getItems().stream()
                .map(item -> new InvoiceLineCommand(
                item.getVariant().getProduct().getName() + " - " + item.getVariant().getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                taxRate,
                item.getSubtotal()))
                .toList();

        String customerName = (customer.getBusinessName() != null && !customer.getBusinessName().isBlank())
                ? customer.getBusinessName()
                : (nullToEmpty(customer.getFirstName()) + " " + nullToEmpty(customer.getLastName())).trim();

        return new CreateInvoiceCommand(
                idempotencyKey,
                customer.getIdType().name(),
                customer.getIdNumber(),
                customerName,
                customer.getEmail(),
                customer.getPhone(),
                order.getAddress().getLine1(),
                lines,
                "N/A",
                baseImponible,
                taxAmount,
                total);
    }

    private Invoice getAuthorizedInvoiceOrThrow(Order order) {
        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aún no se ha generado una factura para este pedido"));
        if (invoice.getStatus() != InvoiceStatus.AUTHORIZED) {
            throw new InvalidInvoiceStateException("La factura no está autorizada todavía (estado: " + invoice.getStatus() + ")");
        }
        return invoice;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
