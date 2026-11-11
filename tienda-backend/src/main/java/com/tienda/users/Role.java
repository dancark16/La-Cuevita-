package com.tienda.users;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
