package com.tienda.orders;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.customers.Address;
import com.tienda.customers.Customer;
import com.tienda.customers.CustomerService;
import com.tienda.inventory.InsufficientStockException;
import com.tienda.inventory.InventoryRepository;
import com.tienda.orders.dto.CheckoutRequest;
import com.tienda.orders.dto.OrderItemRequest;
import com.tienda.orders.dto.OrderResponse;
import com.tienda.products.variant.ProductVariant;
import com.tienda.products.variant.ProductVariantRepository;
import com.tienda.products.variant.VariantStatus;
import com.tienda.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final CouponRepository couponRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, User user) {
        Customer customer = customerService.getOrCreateForUser(user, request.customerProfile());
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new IllegalArgumentException("Indica un teléfono (WhatsApp) para coordinar tu pedido");
        }
        Address address = customerService.addAddress(customer, request.address());

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {
            ProductVariant variant = variantRepository.findById(itemRequest.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada: " + itemRequest.variantId()));

            if (variant.getStatus() != VariantStatus.ACTIVE) {
                throw new IllegalArgumentException("La variante " + variant.getSku() + " no está disponible");
            }

            int availableStock = inventoryRepository.findByVariantId(variant.getId())
                    .map(inv -> inv.getQuantityOnHand())
                    .orElse(0);

            if (availableStock < itemRequest.quantity()) {
                throw new InsufficientStockException(
                        "Stock insuficiente para el SKU %s: disponible %d, solicitado %d"
                                .formatted(variant.getSku(), availableStock, itemRequest.quantity()));
            }

            BigDecimal unitPrice = variant.getPromoPrice() != null ? variant.getPromoPrice() : variant.getPrice();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

            items.add(OrderItem.builder()
                    .variant(variant)
                    .quantity(itemRequest.quantity())
                    .unitPrice(unitPrice)
                    .subtotal(itemSubtotal)
                    .build());

            subtotal = subtotal.add(itemSubtotal);
        }

        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;

        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            coupon = resolveCoupon(request.couponCode());
            discount = calculateDiscount(coupon, subtotal);
        }

        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .customer(customer)
                .address(address)
                .coupon(coupon)
                .orderNumber(orderRepository.nextOrderNumber())
                .paymentMethod("TRANSFER")
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .build();

        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));

        assertOwnership(order, user);

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Order getOwnedOrder(UUID id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));

        assertOwnership(order, user);

        return order;
    }

    @Transactional
    public void markAsPaid(Order order) {
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findMine(User user, Pageable pageable) {
        if (isAdmin(user)) {
            return orderRepository.findAll(pageable).map(orderMapper::toResponse);
        }

        return customerService.findCustomerIdByUserId(user.getId())
                .map(customerId -> orderRepository.findByCustomerId(customerId, pageable).map(orderMapper::toResponse))
                .orElseGet(() -> Page.empty(pageable));
    }

    @Transactional
    public OrderResponse cancel(UUID id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));

        assertOwnership(order, user);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Solo se puede cancelar un pedido en estado PENDING (actual: " + order.getStatus() + ")");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private Coupon resolveCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado: " + code));

        Instant now = Instant.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new IllegalArgumentException("El cupón " + code + " no está vigente");
        }

        if (coupon.getUsageLimit() != null) {
            long used = orderRepository.countByCouponId(coupon.getId());
            if (used >= coupon.getUsageLimit()) {
                throw new IllegalArgumentException("El cupón " + code + " ya alcanzó su límite de uso");
            }
        }

        return coupon;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal rawDiscount = switch (coupon.getDiscountType()) {
            case PERCENTAGE ->
                subtotal.multiply(coupon.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED ->
                coupon.getDiscountValue();
        };
        return rawDiscount.min(subtotal);
    }

    private void assertOwnership(Order order, User user) {
        if (isAdmin(user)) {
            return;
        }
        boolean owns = order.getCustomer().getUser() != null
                && order.getCustomer().getUser().getId().equals(user.getId());
        if (!owns) {
            throw new ResourceNotFoundException("Pedido no encontrado: " + order.getId());
        }
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole().getName());
    }
}
