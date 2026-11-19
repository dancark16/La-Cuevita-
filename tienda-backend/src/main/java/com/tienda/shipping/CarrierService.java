package com.tienda.shipping;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.shipping.dto.ShippingDtos.CarrierRequest;
import com.tienda.shipping.dto.ShippingDtos.CarrierResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository repository;

    @Transactional(readOnly = true)
    public List<CarrierResponse> list() {
        return repository.findAllByOrderBySortOrderAscNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CarrierResponse create(CarrierRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe un transportista con ese nombre");
        }
        Carrier carrier = Carrier.builder()
                .name(name)
                .trackingUrl(clean(request.trackingUrl()))
                .active(request.active() == null || request.active())
                .sortOrder((int) repository.count() + 1)
                .build();
        return toResponse(repository.save(carrier));
    }

    @Transactional
    public CarrierResponse update(UUID id, CarrierRequest request) {
        Carrier carrier = get(id);
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Ya existe un transportista con ese nombre");
        }
        carrier.setName(name);
        carrier.setTrackingUrl(clean(request.trackingUrl()));
        if (request.active() != null) {
            carrier.setActive(request.active());
        }
        return toResponse(repository.save(carrier));
    }

    @Transactional
    public void delete(UUID id) {
        // Los pedidos guardan el nombre del transportista, así que borrarlo no daña el historial
        repository.delete(get(id));
    }

    @Transactional(readOnly = true)
    public Carrier get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transportista no encontrado: " + id));
    }

    private CarrierResponse toResponse(Carrier c) {
        return new CarrierResponse(c.getId(), c.getName(), c.getTrackingUrl(), c.isActive());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
