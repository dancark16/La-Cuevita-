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

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
