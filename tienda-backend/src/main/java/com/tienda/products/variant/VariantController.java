package com.tienda.products.variant;

import com.tienda.products.variant.dto.ImageRequest;
import com.tienda.products.variant.dto.VariantRequest;
import com.tienda.products.variant.dto.VariantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/variants")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    @GetMapping
    public List<VariantResponse> findByProduct(@RequestParam UUID productId) {
        return variantService.findByProduct(productId);
    }

    @GetMapping("/{id}")
    public VariantResponse findById(@PathVariable UUID id) {
        return variantService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse create(@Valid @RequestBody VariantRequest request) {
        return variantService.create(request);
    }

    @PutMapping("/{id}")
    public VariantResponse update(@PathVariable UUID id, @Valid @RequestBody VariantRequest request) {
        return variantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> discontinue(@PathVariable UUID id) {
        variantService.discontinue(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse addImage(@PathVariable UUID id, @Valid @RequestBody ImageRequest request) {
        return variantService.addImage(id, request);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public VariantResponse removeImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return variantService.removeImage(id, imageId);
    }
}
