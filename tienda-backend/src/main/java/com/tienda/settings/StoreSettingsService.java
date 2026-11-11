package com.tienda.settings;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.settings.dto.SettingsDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {

    private static final short SETTINGS_ID = 1;

    private final StoreSettingsRepository settingsRepository;
    private final BankAccountRepository bankRepository;

    @Transactional(readOnly = true)
    public PublicSettings publicSettings() {
        StoreSettings settings = load();
        List<String> names = bankRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(BankAccount::getBankName)
                .distinct()
                .toList();
        return new PublicSettings(settings.getWhatsappNumber(), settings.getInstagramUser(),
                settings.isPayphoneEnabled(), names);
    }

    @Transactional(readOnly = true)
    public PaymentInfo paymentInfo() {
        return new PaymentInfo(bankRepository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public AdminSettings adminSettings() {
        StoreSettings settings = load();
        return new AdminSettings(settings.getWhatsappNumber(), settings.getInstagramUser(),
                settings.isPayphoneEnabled(),
                bankRepository.findAllByOrderBySortOrderAscCreatedAtAsc().stream().map(this::toResponse).toList());
    }

    @Transactional
    public AdminSettings update(SettingsRequest request) {
        StoreSettings settings = load();
        settings.setWhatsappNumber(normalizeWhatsapp(request.whatsappNumber()));
        settings.setInstagramUser(normalizeInstagram(request.instagramUser()));
        settings.setPayphoneEnabled(request.payphoneEnabled());
        settingsRepository.save(settings);
        return adminSettings();
    }

    @Transactional
    public BankAccountResponse createBank(BankAccountRequest request) {
        BankAccount account = BankAccount.builder()
                .sortOrder((int) bankRepository.count())
                .active(request.active() == null || request.active())
                .build();
        apply(account, request);
        return toResponse(bankRepository.save(account));
    }

    @Transactional
    public BankAccountResponse updateBank(UUID id, BankAccountRequest request) {
        BankAccount account = bankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada: " + id));
        apply(account, request);
        if (request.active() != null) {
            account.setActive(request.active());
        }
        return toResponse(bankRepository.save(account));
    }

    @Transactional
    public void deleteBank(UUID id) {
        BankAccount account = bankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada: " + id));
        bankRepository.delete(account);
    }

    private void apply(BankAccount account, BankAccountRequest request) {
        account.setBankName(request.bankName().trim());
        account.setAccountType(request.accountType().trim());
        account.setAccountNumber(request.accountNumber().trim());
        account.setHolder(request.holder().trim());
        account.setHolderId(blankToNull(request.holderId()));
        account.setHolderEmail(blankToNull(request.holderEmail()));
        account.setSwiftCode(blankToNull(request.swiftCode()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StoreSettings load() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(() ->
                settingsRepository.save(StoreSettings.builder().id(SETTINGS_ID).payphoneEnabled(false).build()));
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(account.getId(), account.getBankName(), account.getAccountType(),
                account.getAccountNumber(), account.getHolder(), account.getHolderId(),
                account.getHolderEmail(), account.getSwiftCode(), account.isActive());
    }

    /** Acepta 0991234567, +593 99 123 4567 o 593991234567 y lo deja como 593991234567. */
    private static String normalizeWhatsapp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("593")) {
            // ya viene con código de país
        } else if (digits.startsWith("0")) {
            digits = "593" + digits.substring(1);
        } else {
            digits = "593" + digits;
        }
        if (digits.length() < 11 || digits.length() > 13) {
            throw new IllegalArgumentException("El número de WhatsApp no es válido. Ejemplo: 0991234567");
        }
        return digits;
    }

    private static String normalizeInstagram(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String user = raw.trim().replaceFirst("^@", "");
        if (!user.matches("[A-Za-z0-9._]{1,30}")) {
            throw new IllegalArgumentException("El usuario de Instagram no es válido.");
        }
        return user;
    }
}
