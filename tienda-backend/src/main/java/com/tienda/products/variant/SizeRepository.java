package com.tienda.products.variant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SizeRepository extends JpaRepository<Size, UUID> {
    boolean existsByLabel(String label);
    boolean existsByLabelAndIdNot(String label, UUID id);

    Optional<Size> findByLabel(String label);
}
