package com.tienda.imports;

import com.tienda.imports.dto.ImportResult;
import com.tienda.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/imports")
@RequiredArgsConstructor
public class ProductImportController {

    private final ProductImportService importService;

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return importService.run(file, dryRun, principal.getUser());
    }
}