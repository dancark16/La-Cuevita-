package com.tienda.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ShippingDtos {

    private ShippingDtos() {
    }

    public record CarrierRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 300) String trackingUrl,
            Boolean active
    ) {
    }

    public record CarrierResponse(UUID id, String name, String trackingUrl, boolean active) {
    }

    /** Datos al marcar un pedido como enviado. */
    public record ShipRequest(
            UUID carrierId,
            @Size(max = 80) String trackingNumber,
            @Size(max = 500) String note
    ) {
    }

    public record TrackingEventRequest(
            @NotBlank @Size(max = 300) String message,
            boolean notifyCustomer
    ) {
    }

    public record TrackingEventResponse(UUID id, String message, Instant createdAt) {
    }

    /** Lo que ve la clienta (y el administrador) sobre el envío de un pedido. */
    public record TrackingInfo(
            String carrierName,
            String trackingNumber,
            String trackingUrl,
            Instant shippedAt,
            Instant deliveredAt,
            List<TrackingEventResponse> events
    ) {
    }
}
