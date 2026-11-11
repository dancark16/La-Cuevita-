package com.tienda.orders.dto;

import com.tienda.customers.dto.AddressRequest;
import com.tienda.customers.dto.CustomerProfileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(
        @Valid
        CustomerProfileRequest customerProfile,
        @NotNull
        @Valid
        AddressRequest address,
        @NotEmpty
        @Valid
        List<OrderItemRequest> items,
        String couponCode
        ) {

}
