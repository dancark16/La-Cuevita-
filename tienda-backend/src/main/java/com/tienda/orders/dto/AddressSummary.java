package com.tienda.orders.dto;

public record AddressSummary(
        String line1,
        String line2,
        String city,
        String province,
        String reference
        ) {

}
