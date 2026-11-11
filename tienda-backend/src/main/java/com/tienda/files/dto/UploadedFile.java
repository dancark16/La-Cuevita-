package com.tienda.files.dto;

import java.util.UUID;

public record UploadedFile(UUID id, String url, String originalName, long sizeBytes) {
}
