package product_service.service;

import product_service.dto.PageResponse;
import product_service.dto.ProductRequest;
import product_service.dto.ProductResponse;
import product_service.entity.Category;
import product_service.entity.Product;
import product_service.exception.ApiException;
import product_service.repository.CategoryRepository;
import product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse create(Long userId, String role, ProductRequest request) {
        requireSellerOrAdmin(role);

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .type(request.type())
                .price(request.price())
                .weightGrams(request.weightGrams())
                .imageUrl(request.imageUrl())
                .sellerId(userId)
                .category(resolveCategory(request.categoryId()))
                .active(true)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<Product> result = productRepository.findByActiveTrue(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")));

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public ProductResponse update(Long id, Long userId, String role, ProductRequest request) {
        requireSellerOrAdmin(role);
        Product product = findActive(id);
        requireOwnerOrAdmin(product, userId, role);

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setType(request.type());
        product.setPrice(request.price());
        product.setWeightGrams(request.weightGrams());
        product.setImageUrl(request.imageUrl());
        product.setCategory(resolveCategory(request.categoryId()));

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id, Long userId, String role) {
        requireSellerOrAdmin(role);
        Product product = findActive(id);
        requireOwnerOrAdmin(product, userId, role);

        product.setActive(false); // soft delete
        productRepository.save(product);
    }

    private Product findActive(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    private void requireSellerOrAdmin(String role) {
        if (!"SELLER".equals(role) && !"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only SELLER or ADMIN can manage products");
        }
    }

    private void requireOwnerOrAdmin(Product product, Long userId, String role) {
        if (!"ADMIN".equals(role) && !product.getSellerId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can modify only your own products");
        }
    }

    private ProductResponse toResponse(Product p) {
        Category c = p.getCategory();
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getType(),
                p.getPrice(),
                p.getWeightGrams(),
                p.getImageUrl(),
                p.getSellerId(),
                c == null ? null : c.getId(),
                c == null ? null : c.getName(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
