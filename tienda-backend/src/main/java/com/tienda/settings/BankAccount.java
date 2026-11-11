package com.tienda.settings;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_name", nullable = false, length = 80)
    private String bankName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "account_number", nullable = false, length = 40)
    private String accountNumber;

    @Column(nullable = false, length = 120)
    private String holder;

    @Column(name = "holder_id", length = 20)
    private String holderId;

    @Column(name = "holder_email", length = 150)
    private String holderEmail;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
