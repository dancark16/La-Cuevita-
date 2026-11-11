package com.tienda.products.variant;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.products.variant.dto.SizeRequest;
import com.tienda.products.variant.dto.SizeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SizeService {

    private final SizeRepository sizeRepository;

    @Transactional(readOnly = true)
    public List<SizeResponse> findAll() {
        return sizeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SizeResponse create(SizeRequest request) {
        if (sizeRepository.existsByLabel(request.label())) {
            throw new IllegalArgumentException("Ya existe la talla: " + request.label());
        }
        Size size = Size.builder().label(request.label()).active(request.active() == null || request.active()).build();
        return toResponse(sizeRepository.save(size));
    }

    @Transactional
    public SizeResponse update(UUID id, SizeRequest request) {
        Size size = getOrThrow(id);
        if (sizeRepository.existsByLabelAndIdNot(request.label(), id)) {
            throw new IllegalArgumentException("Ya existe otra talla con esa etiqueta: " + request.label());
        }
        size.setLabel(request.label());
        if (request.active() != null) {
            size.setActive(request.active());
        }
        return toResponse(sizeRepository.save(size));
    }

    @Transactional
    public void delete(UUID id) {
        sizeRepository.delete(getOrThrow(id));
    }

    private Size getOrThrow(UUID id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Talla no encontrada: " + id));
    }

    private SizeResponse toResponse(Size size) {
        return new SizeResponse(size.getId(), size.getLabel(), size.isActive());
    }
}
