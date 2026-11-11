package com.tienda.products.variant;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.products.variant.dto.ColorRequest;
import com.tienda.products.variant.dto.ColorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    @Transactional(readOnly = true)
    public List<ColorResponse> findAll() {
        return colorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ColorResponse create(ColorRequest request) {
        if (colorRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Ya existe el color: " + request.name());
        }
        Color color = Color.builder().name(request.name()).hex(request.hex()).active(request.active() == null || request.active()).build();
        return toResponse(colorRepository.save(color));
    }

    @Transactional
    public ColorResponse update(UUID id, ColorRequest request) {
        Color color = getOrThrow(id);
        if (colorRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("Ya existe otro color con ese nombre: " + request.name());
        }
        color.setName(request.name());
        color.setHex(request.hex());
        if (request.active() != null) {
            color.setActive(request.active());
        }
        return toResponse(colorRepository.save(color));
    }

    @Transactional
    public void delete(UUID id) {
        colorRepository.delete(getOrThrow(id));
    }

    private Color getOrThrow(UUID id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color no encontrado: " + id));
    }

    private ColorResponse toResponse(Color color) {
        return new ColorResponse(color.getId(), color.getName(), color.getHex(), color.isActive());
    }
}
