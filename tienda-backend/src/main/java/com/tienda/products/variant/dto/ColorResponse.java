package com.tienda.products.variant.dto;

import java.util.UUID;

public record ColorResponse(UUID id, String name, String hex, boolean active) {

}
