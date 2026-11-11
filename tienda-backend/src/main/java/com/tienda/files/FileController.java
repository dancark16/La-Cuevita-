package com.tienda.files;

import com.tienda.files.dto.UploadedFile;
import com.tienda.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController {

    private static final int MAX_FILES_PER_REQUEST = 20;

    private final FileStorageService storageService;

    @PostMapping(value = "/api/admin/files/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<UploadedFile> uploadProductImages(
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (files.isEmpty() || files.size() > MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException("Sube entre 1 y " + MAX_FILES_PER_REQUEST + " imágenes a la vez.");
        }
        return files.stream()
                .map(file -> storageService.storeProductImage(file, principal.getUser()))
                .toList();
    }

    @GetMapping("/api/files/{id}")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        StoredFile file = storageService.getPublicImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(new FileSystemResource(storageService.pathOf(file)));
    }
}
