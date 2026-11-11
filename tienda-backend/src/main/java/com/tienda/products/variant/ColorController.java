package com.tienda.products.variant;

import com.tienda.products.variant.dto.ColorRequest;
import com.tienda.products.variant.dto.ColorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @GetMapping
    public List<ColorResponse> findAll() {
        return colorService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColorResponse create(@Valid @RequestBody ColorRequest request) {
        return colorService.create(request);
    }

    @PutMapping("/{id}")
    public ColorResponse update(@PathVariable UUID id, @Valid @RequestBody ColorRequest request) {
        return colorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        colorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
