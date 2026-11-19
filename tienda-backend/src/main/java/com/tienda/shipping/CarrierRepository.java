package com.tienda.shipping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CarrierRepository extends JpaRepository<Carrier, UUID> {

    List<Carrier> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
