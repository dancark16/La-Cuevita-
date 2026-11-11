package com.tienda.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class SettingsDtos {

    private SettingsDtos() {
    }

    /** Lo que ve cualquier visitante de la tienda. No incluye números de cuenta. */
    public record PublicSettings(
            String whatsappNumber,
            String instagramUser,
            boolean payphoneEnabled,
            List<String> bankNames
    ) {
    }

    /** Datos para transferir; solo para clientas con sesión iniciada. */
    public record PaymentInfo(List<BankAccountResponse> bankAccounts) {
    }

    public record AdminSettings(
            String whatsappNumber,
            String instagramUser,
            boolean payphoneEnabled,
            List<BankAccountResponse> bankAccounts
    ) {
    }

    public record SettingsRequest(
            @Size(max = 20) String whatsappNumber,
            @Size(max = 60) String instagramUser,
            boolean payphoneEnabled
    ) {
    }

    public record BankAccountRequest(
            @NotBlank @Size(max = 80) String bankName,
            @NotBlank @Size(max = 20) String accountType,
            @NotBlank @Size(max = 40) String accountNumber,
            @NotBlank @Size(max = 120) String holder,
            @Size(max = 20) String holderId,
            @Size(max = 150) String holderEmail,
            @Size(max = 20) String swiftCode,
            Boolean active
    ) {
    }

    public record BankAccountResponse(
            UUID id,
            String bankName,
            String accountType,
            String accountNumber,
            String holder,
            String holderId,
            String holderEmail,
            String swiftCode,
            boolean active
    ) {
    }
}
