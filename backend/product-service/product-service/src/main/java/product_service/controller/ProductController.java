package product_service.controller;

import product_service.dto.PageResponse;
import product_service.dto.ProductRequest;
import product_service.dto.ProductResponse;
import product_service.entity.ProductType;
import product_service.security.ProductPrincipal;
import product_service.service.ImageStorageService;
import product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorageService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @AuthenticationPrincipal ProductPrincipal principal,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(principal.userId(), principal.role(), request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(productService.list(
                page, size, search, type, minPrice, maxPrice, categoryId, sortBy, direction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal ProductPrincipal principal,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(
                id, principal.userId(), principal.role(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal ProductPrincipal principal) {
        productService.delete(id, principal.userId(), principal.role());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> uploadImage(
            @PathVariable Long id,
            @AuthenticationPrincipal ProductPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadImage(
                id, principal.userId(), principal.role(), file));
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        Resource resource = imageStorageService.load(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageStorageService.contentType(filename)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
