package com.tienda.payments;

import com.tienda.inventory.InventoryService;
import com.tienda.inventory.MovementType;
import com.tienda.inventory.dto.MovementRequest;
import com.tienda.orders.InvalidOrderStateException;
import com.tienda.orders.Order;
import com.tienda.orders.OrderItem;
import com.tienda.orders.OrderService;
import com.tienda.orders.OrderStatus;
import com.tienda.payments.dto.PaymentRequest;
import com.tienda.payments.dto.PaymentResponse;
import com.tienda.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tienda.invoices.InvoiceService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final InvoiceService invoiceService;

    @Transactional
    public PaymentResponse pay(UUID orderId, PaymentRequest request, User user) {
        Order order = orderService.getOwnedOrder(orderId, user);

        String idempotencyKey = orderId.toString();

        java.util.Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return paymentMapper.toResponse(existing.get());
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Solo se puede pagar un pedido en estado PENDING (actual: " + order.getStatus() + ")");
        }

        return processPayment(order, request, idempotencyKey, user);
    }

    /** El administrador confirma que la transferencia llegó: registra el pago, descuenta stock y pide la factura. */
    @Transactional
    public PaymentResponse confirmTransfer(Order order, User admin) {
        String idempotencyKey = order.getId().toString();

        java.util.Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return paymentMapper.toResponse(existing.get());
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Solo se puede confirmar el pago de un pedido PENDING (actual: " + order.getStatus() + ")");
        }
        return processPayment(order, new PaymentRequest("TRANSFER", PaymentStatus.APPROVED), idempotencyKey, admin);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByOrder(UUID orderId, User user) {
        Order order = orderService.getOwnedOrder(orderId, user);
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    private PaymentResponse processPayment(Order order, PaymentRequest request, String idempotencyKey, User user) {
        PaymentStatus status = request.simulateStatus() != null ? request.simulateStatus() : PaymentStatus.APPROVED;

        Payment payment = Payment.builder()
                .order(order)
                .method(request.method())
                .status(status)
                .amount(order.getTotal())
                .idempotencyKey(idempotencyKey)
                .build();

        payment = paymentRepository.save(payment);

        if (status == PaymentStatus.APPROVED) {
            deductInventory(order, user);
            orderService.markAsPaid(order);
            invoiceService.requestInvoice(order);   // <-- nueva línea
        }

        return paymentMapper.toResponse(payment);
    }

    private void deductInventory(Order order, User user) {
        for (OrderItem item : order.getItems()) {
            inventoryService.registerMovement(
                    new MovementRequest(
                            item.getVariant().getId(),
                            MovementType.VENTA,
                            item.getQuantity(),
                            "Venta por pedido " + order.getId(),
                            order.getId().toString(),
                            order.getId()),
                    user);
        }
    }
}
