package com.tienda.orders;

import com.tienda.orders.dto.AdminOrderSummary;
import com.tienda.orders.dto.OrderNoteRequest;
import com.tienda.orders.dto.OrderResponse;
import com.tienda.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderTransferService service;

    @GetMapping
    public Page<AdminOrderSummary> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(status, q, pageable);
    }

    @GetMapping("/stats")
    public Map<OrderStatus, Long> stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<Resource> receipt(@PathVariable UUID id,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        OrderTransferService.ReceiptFile receipt = service.receiptOf(id, principal.getUser());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(receipt.path()));
    }

    @PostMapping("/{id}/confirm-payment")
    public OrderResponse confirmPayment(@PathVariable UUID id,
                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return service.confirmPayment(id, principal.getUser());
    }

    @PostMapping("/{id}/reject-receipt")
    public OrderResponse rejectReceipt(@PathVariable UUID id, @Valid @RequestBody OrderNoteRequest request) {
        return service.rejectReceipt(id, request.note());
    }

    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable UUID id, @Valid @RequestBody OrderNoteRequest request) {
        return service.ship(id, request.note());
    }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable UUID id) {
        return service.deliver(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody OrderNoteRequest request) {
        return service.cancel(id, request.note());
    }
}
