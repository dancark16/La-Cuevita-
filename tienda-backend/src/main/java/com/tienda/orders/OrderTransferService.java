package com.tienda.orders;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.files.FileStorageService;
import com.tienda.files.StoredFile;
import com.tienda.orders.dto.AdminOrderSummary;
import com.tienda.orders.dto.OrderResponse;
import com.tienda.payments.PaymentService;
import com.tienda.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Flujo de pago por transferencia: la clienta sube el comprobante y el administrador lo confirma. */
@Service
@RequiredArgsConstructor
public class OrderTransferService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final FileStorageService fileStorage;
    private final PaymentService paymentService;
    private final com.tienda.shipping.ShippingService shippingService;

    @org.springframework.beans.factory.annotation.Value("${app.orders.allow-delete:false}")
    private boolean allowDelete;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    // ---- clienta ----

    @Transactional
    public OrderResponse attachReceipt(UUID orderId, MultipartFile file, User user) {
        Order order = orderService.getOwnedOrder(orderId, user);
        requireStatus(order, OrderStatus.PENDING, "Solo se puede subir el comprobante de un pedido pendiente de pago");

        StoredFile stored = fileStorage.storeReceipt(file, user);
        order.setReceiptFile(stored);
        order.setReceiptUploadedAt(Instant.now());
        order.setAdminNote(null);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    /** Devuelve el archivo del comprobante; solo su dueña o un administrador pueden verlo. */
    @Transactional(readOnly = true)
    public ReceiptFile receiptOf(UUID orderId, User user) {
        Order order = orderService.getOwnedOrder(orderId, user);
        StoredFile file = order.getReceiptFile();
        if (file == null) {
            throw new ResourceNotFoundException("Este pedido no tiene comprobante");
        }
        return new ReceiptFile(file.getContentType(), fileStorage.pathOf(file));
    }

    public record ReceiptFile(String contentType, Path path) {
    }

    // ---- administrador ----

    @Transactional(readOnly = true)
    public Page<AdminOrderSummary> search(String status, String q, Pageable pageable) {
        String normalizedQuery = (q == null || q.isBlank()) ? null : q.trim();
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        return orderRepository.searchAdmin(normalizedStatus, normalizedQuery, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return orderMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> stats() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : orderRepository.countByStatus()) {
            counts.put((OrderStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional
    public OrderResponse confirmPayment(UUID id, User admin) {
        Order order = getOrThrow(id);
        paymentService.confirmTransfer(order, admin);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse rejectReceipt(UUID id, String reason) {
        Order order = getOrThrow(id);
        requireStatus(order, OrderStatus.PENDING, "Solo se puede rechazar el comprobante de un pedido pendiente");
        if (order.getReceiptFile() == null) {
            throw new InvalidOrderStateException("Este pedido no tiene comprobante que rechazar");
        }
        order.setReceiptFile(null);
        order.setReceiptUploadedAt(null);
        order.setAdminNote(clean(reason, "Comprobante rechazado"));
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse ship(UUID id, com.tienda.shipping.dto.ShippingDtos.ShipRequest request) {
        String note = request.note();
        Order order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROCESSING) {
            throw new InvalidOrderStateException(
                    "Solo se puede enviar un pedido pagado (actual: " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(Instant.now());
        if (note != null && !note.isBlank()) {
            order.setAdminNote(note.trim());
        }
        shippingService.markShipped(order, request);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse deliver(UUID id) {
        Order order = getOrThrow(id);
        requireStatus(order, OrderStatus.SHIPPED, "Solo se puede marcar como entregado un pedido enviado");
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        shippingService.markDelivered(order);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(UUID id, String reason) {
        Order order = getOrThrow(id);
        requireStatus(order, OrderStatus.PENDING, "Solo se puede cancelar un pedido pendiente de pago");
        order.setStatus(OrderStatus.CANCELLED);
        order.setAdminNote(clean(reason, "Pedido cancelado"));
        orderService.releaseReservation(order);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse addTrackingEvent(UUID id, String message, boolean notify) {
        Order order = getOrThrow(id);
        shippingService.addEvent(order, message, notify);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse deleteTrackingEvent(UUID id, UUID eventId) {
        Order order = getOrThrow(id);
        shippingService.deleteEvent(id, eventId);
        return orderMapper.toResponse(order);
    }

    /** Cancela un pedido pendiente cuya reserva venció sin recibir el pago. */
    @Transactional
    public void expire(UUID id) {
        Order order = getOrThrow(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setAdminNote("La reserva venció: no se recibió el pago a tiempo");
        orderService.releaseReservation(order);
        orderRepository.save(order);
    }

    /**
     * Borra un pedido por completo (para pruebas). Si ya había descontado stock, lo devuelve;
     * también elimina su pago, factura, envío y comprobante.
     */
    @Transactional
    public void delete(UUID id) {
        if (!allowDelete) {
            throw new InvalidOrderStateException("Borrar pedidos está desactivado en este entorno");
        }
        Order order = getOrThrow(id);
        if (order.getStatus() == OrderStatus.PENDING) {
            orderService.releaseReservation(order);
        }
        StoredFile receipt = order.getReceiptFile();
        UUID receiptId = receipt != null ? receipt.getId() : null;
        Path receiptPath = receipt != null ? fileStorage.pathOf(receipt) : null;

        // Devuelve al inventario lo que este pedido había descontado
        entityManager.createNativeQuery(
                "UPDATE inventory i SET quantity_on_hand = quantity_on_hand + m.q "
                        + "FROM (SELECT inventory_id, SUM(quantity) AS q FROM inventory_movements "
                        + "WHERE order_id = :id AND type = 'VENTA' GROUP BY inventory_id) m "
                        + "WHERE i.id = m.inventory_id")
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM inventory_movements WHERE order_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM invoices WHERE order_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM payments WHERE order_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM shipping WHERE order_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        orderRepository.deleteById(id);
        orderRepository.flush();

        if (receiptId != null && orderRepository.countByReceiptFileId(receiptId) == 0) {
            fileStorage.removeById(receiptId, receiptPath);
        }
    }

    private AdminOrderSummary toSummary(Order order) {
        return new AdminOrderSummary(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                orderMapper.customerName(order),
                order.getCustomer().getPhone(),
                order.getTotal(),
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                order.getReceiptFile() != null,
                order.getReceiptUploadedAt(),
                order.getCreatedAt());
    }

    private Order getOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
    }

    private static void requireStatus(Order order, OrderStatus expected, String message) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(message + " (actual: " + order.getStatus() + ")");
        }
    }

    private static String clean(String note, String fallback) {
        return note == null || note.isBlank() ? fallback : note.trim();
    }
}
