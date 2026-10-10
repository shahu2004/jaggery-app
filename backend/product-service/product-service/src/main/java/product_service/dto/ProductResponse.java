package product_service.dto;

import product_service.entity.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        ProductType type,
        BigDecimal price,
        Integer weightGrams,
        String imageUrl,
        Long sellerId,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
