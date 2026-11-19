package com.tienda.shipping;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Un transportista (o forma de entrega) que ofrece la tienda. */
@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** Enlace de rastreo; {guia} se reemplaza por el número de guía. */
    @Column(name = "tracking_url", length = 300)
    private String trackingUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
