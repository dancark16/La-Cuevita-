package com.tienda.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Cancela los pedidos que siguen sin pagarse pasado el tiempo de reserva, para liberar su stock. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReservationExpirer {

    private final OrderRepository orderRepository;
    private final OrderTransferService transferService;

    /** Horas que se aparta el stock de un pedido sin pagar. 0 desactiva el vencimiento. */
    @Value("${app.orders.reservation-hours:48}")
    private int reservationHours;

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    public void expireUnpaidOrders() {
        if (reservationHours <= 0) {
            return;
        }
        Instant limit = Instant.now().minus(Duration.ofHours(reservationHours));
        for (Order order : orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, limit)) {
            try {
                transferService.expire(order.getId());
                log.info("Pedido #{} cancelado: la reserva venció", order.getOrderNumber());
            } catch (RuntimeException e) {
                log.warn("No se pudo vencer el pedido {}", order.getId(), e);
            }
        }
    }
}
