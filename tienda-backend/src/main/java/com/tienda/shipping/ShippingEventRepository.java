package com.tienda.shipping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShippingEventRepository extends JpaRepository<ShippingEvent, UUID> {

    List<ShippingEvent> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
