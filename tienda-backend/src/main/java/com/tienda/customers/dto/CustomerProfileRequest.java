package com.tienda.customers.dto;

import com.tienda.customers.IdentificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerProfileRequest(
        @NotNull
        IdentificationType idType,
        @NotBlank
        String idNumber,
        String firstName,
        String lastName,
        String businessName,
        String phone
        ) {

}
