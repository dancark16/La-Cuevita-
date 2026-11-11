package com.tienda.invoices;

import com.tienda.invoices.dto.InvoiceResponse;
import com.tienda.orders.Order;
import com.tienda.orders.OrderService;
import com.tienda.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrderService orderService;
    private final InvoiceMapper invoiceMapper;

    @GetMapping
    public InvoiceResponse get(@PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        Order order = orderService.getOwnedOrder(orderId, principal.getUser());
        return invoiceMapper.toResponse(invoiceService.getByOrder(order));
    }

    @PostMapping("/retry")
    public InvoiceResponse retry(@PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        Order order = orderService.getOwnedOrder(orderId, principal.getUser());
        return invoiceMapper.toResponse(invoiceService.retry(order));
    }

    @GetMapping("/xml")
    public ResponseEntity<byte[]> downloadXml(@PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        Order order = orderService.getOwnedOrder(orderId, principal.getUser());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(invoiceService.downloadXml(order));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        Order order = orderService.getOwnedOrder(orderId, principal.getUser());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(invoiceService.downloadPdf(order));
    }
}
