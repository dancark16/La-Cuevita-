package com.tienda.invoices;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByOrderId(UUID orderId);

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);
}
