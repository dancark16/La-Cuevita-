package com.tienda.orders;

import com.tienda.orders.dto.AddressSummary;
import com.tienda.orders.dto.OrderItemResponse;
import com.tienda.orders.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        AddressSummary address = new AddressSummary(
                order.getAddress().getLine1(),
                order.getAddress().getLine2(),
                order.getAddress().getCity(),
                order.getAddress().getProvince(),
                order.getAddress().getReference());

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCustomer().getId(),
                buildCustomerName(order),
                order.getCustomer().getEmail(),
                order.getCustomer().getPhone(),
                order.getCustomer().getIdNumber(),
                address,
                items,
                order.getSubtotal(),
                order.getDiscount(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getReceiptFile() != null,
                order.getReceiptUploadedAt(),
                order.getAdminNote(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        var variant = item.getVariant();
        return new OrderItemResponse(
                variant.getId(),
                variant.getSku(),
                variant.getProduct().getName(),
                variant.getSize().getLabel(),
                variant.getColor().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal());
    }

    public String customerName(Order order) {
        return buildCustomerName(order);
    }

    private String buildCustomerName(Order order) {
        var customer = order.getCustomer();
        if (customer.getBusinessName() != null && !customer.getBusinessName().isBlank()) {
            return customer.getBusinessName();
        }
        String first = customer.getFirstName() != null ? customer.getFirstName() : "";
        String last = customer.getLastName() != null ? customer.getLastName() : "";
        return (first + " " + last).trim();
    }
}
