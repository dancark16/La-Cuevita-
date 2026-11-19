package com.tienda.settings;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Configuración general de la tienda. Siempre hay una sola fila (id = 1). */
@Entity
@Table(name = "store_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettings {

    @Id
    private Short id;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "instagram_user", length = 60)
    private String instagramUser;

    @Column(name = "payphone_enabled", nullable = false)
    private boolean payphoneEnabled;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(length = 13)
    private String ruc;

    @Column(name = "head_office_address", length = 300)
    private String headOfficeAddress;

    @Column(name = "branch_address", length = 300)
    private String branchAddress;

    @Column(name = "special_taxpayer", length = 30)
    private String specialTaxpayer;

    @Column(name = "accounting_required", nullable = false)
    private boolean accountingRequired;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
