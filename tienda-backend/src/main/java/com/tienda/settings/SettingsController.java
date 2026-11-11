package com.tienda.settings;

import com.tienda.settings.dto.SettingsDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SettingsController {

    private final StoreSettingsService service;

    /** Público: WhatsApp, Instagram, PayPhone y nombres de los bancos. */
    @GetMapping("/api/store/settings")
    public PublicSettings publicSettings() {
        return service.publicSettings();
    }

    /** Público: las clientas ven los números de cuenta en el carrito para poder transferir. */
    @GetMapping("/api/payment-info")
    public PaymentInfo paymentInfo() {
        return service.paymentInfo();
    }

    @GetMapping("/api/admin/settings")
    public AdminSettings adminSettings() {
        return service.adminSettings();
    }

    @PutMapping("/api/admin/settings")
    public AdminSettings update(@Valid @RequestBody SettingsRequest request) {
        return service.update(request);
    }

    @PostMapping("/api/admin/settings/bank-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse createBank(@Valid @RequestBody BankAccountRequest request) {
        return service.createBank(request);
    }

    @PutMapping("/api/admin/settings/bank-accounts/{id}")
    public BankAccountResponse updateBank(@PathVariable UUID id, @Valid @RequestBody BankAccountRequest request) {
        return service.updateBank(id, request);
    }

    @DeleteMapping("/api/admin/settings/bank-accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBank(@PathVariable UUID id) {
        service.deleteBank(id);
    }
}
