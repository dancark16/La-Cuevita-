package com.tienda.orders;

import com.tienda.orders.dto.OrderResponse;
import com.tienda.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Comprobante de transferencia de un pedido, desde el lado de la clienta. */
@RestController
@RequestMapping("/api/orders/{id}/receipt")
@RequiredArgsConstructor
public class OrderReceiptController {

    private final OrderTransferService transferService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrderResponse upload(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return transferService.attachReceipt(id, file, principal.getUser());
    }

    @GetMapping
    public ResponseEntity<Resource> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        OrderTransferService.ReceiptFile receipt = transferService.receiptOf(id, principal.getUser());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(receipt.path()));
    }
}
