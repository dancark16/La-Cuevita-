package com.tienda.shipping;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.orders.InvalidOrderStateException;
import com.tienda.orders.Order;
import com.tienda.orders.OrderStatus;
import com.tienda.shipping.dto.ShippingDtos.ShipRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final CarrierService carrierService;
    private final ShippingEventRepository eventRepository;
    private final ShippingNotificationService notifications;

    /** Registra el envío: transportista, guía y enlace de rastreo, más la primera novedad. */
    @Transactional
    public void markShipped(Order order, ShipRequest request) {
        Carrier carrier = request.carrierId() != null ? carrierService.get(request.carrierId()) : null;
        String guide = request.trackingNumber() == null || request.trackingNumber().isBlank()
                ? null : request.trackingNumber().trim();

        order.setCarrierName(carrier != null ? carrier.getName() : null);
        order.setTrackingNumber(guide);
        order.setTrackingUrl(carrier != null ? buildUrl(carrier.getTrackingUrl(), guide) : null);

        String text = "Tu pedido salió de la tienda";
        if (carrier != null) {
            text = "Enviado con " + carrier.getName() + (guide != null ? " · guía " + guide : "");
        }
        eventRepository.save(ShippingEvent.builder().orderId(order.getId()).message(text).build());
        notifications.notifyShippedAfterCommit(order.getId());
    }

    @Transactional
    public void markDelivered(Order order) {
        eventRepository.save(ShippingEvent.builder().orderId(order.getId()).message("Pedido entregado").build());
        notifications.notifyUpdateAfterCommit(order.getId(), "Tu pedido fue entregado. ¡Gracias por comprar en La Cuevita!");
    }

    @Transactional
    public ShippingEvent addEvent(Order order, String message, boolean notify) {
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException("Solo se pueden agregar novedades a un pedido enviado");
        }
        ShippingEvent event = eventRepository.save(
                ShippingEvent.builder().orderId(order.getId()).message(message.trim()).build());
        if (notify) {
            notifications.notifyUpdateAfterCommit(order.getId(), message.trim());
        }
        return event;
    }

    @Transactional
    public void deleteEvent(UUID orderId, UUID eventId) {
        ShippingEvent event = eventRepository.findById(eventId)
                .filter(e -> e.getOrderId().equals(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Novedad no encontrada"));
        eventRepository.delete(event);
    }

    /** Reemplaza {guia} en la plantilla del transportista; sin plantilla no hay enlace. */
    static String buildUrl(String template, String guide) {
        if (template == null || template.isBlank()) {
            return null;
        }
        if (template.contains("{guia}")) {
            return guide == null ? null : template.replace("{guia}", URLEncoder.encode(guide, StandardCharsets.UTF_8));
        }
        return template;
    }
}
