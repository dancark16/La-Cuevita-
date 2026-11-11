package com.tienda.products.variant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ColorRepository extends JpaRepository<Color, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<Color> findByName(String name);
}
