package com.tienda.products.variant;

import com.tienda.products.variant.dto.SizeRequest;
import com.tienda.products.variant.dto.SizeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sizes")
@RequiredArgsConstructor
public class SizeController {

    private final SizeService sizeService;

    @GetMapping
    public List<SizeResponse> findAll() {
        return sizeService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SizeResponse create(@Valid @RequestBody SizeRequest request) {
        return sizeService.create(request);
    }

    @PutMapping("/{id}")
    public SizeResponse update(@PathVariable UUID id, @Valid @RequestBody SizeRequest request) {
        return sizeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sizeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
