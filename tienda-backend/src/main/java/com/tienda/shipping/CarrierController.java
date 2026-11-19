package com.tienda.shipping;

import com.tienda.shipping.dto.ShippingDtos.CarrierRequest;
import com.tienda.shipping.dto.ShippingDtos.CarrierResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService service;

    @GetMapping
    public List<CarrierResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarrierResponse create(@Valid @RequestBody CarrierRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CarrierResponse update(@PathVariable UUID id, @Valid @RequestBody CarrierRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
