package com.tienda.customers.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank
        String line1,
        String line2,
        @NotBlank
        String city,
        @NotBlank
        String province,
        String reference
        ) {

}
