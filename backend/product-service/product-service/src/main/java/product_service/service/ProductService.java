package product_service.service;

import product_service.dto.PageResponse;
import product_service.dto.ProductRequest;
import product_service.dto.ProductResponse;
import product_service.entity.Category;
import product_service.entity.Product;
import product_service.entity.ProductType;
import product_service.exception.ApiException;
import product_service.repository.CategoryRepository;
import product_service.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "createdAt");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

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
    public PageResponse<ProductResponse> list(int page, int size, String search, ProductType type,
                                              BigDecimal minPrice, BigDecimal maxPrice, Long categoryId,
                                              String sortBy, String direction) {

        if (search != null && search.length() > MAX_SEARCH_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Search text must be at most 100 characters");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "minPrice cannot be greater than maxPrice");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<Product> result = productRepository.findAll(
                buildSpec(search, type, minPrice, maxPrice, categoryId),
                PageRequest.of(safePage, safeSize, buildSort(sortBy, direction)));

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

        String oldImage = product.getImageUrl();
        boolean imageReplaced = request.imageUrl() != null && !request.imageUrl().equals(oldImage);

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setType(request.type());
        product.setPrice(request.price());
        product.setWeightGrams(request.weightGrams());
        product.setCategory(resolveCategory(request.categoryId()));
        if (imageReplaced) {
            product.setImageUrl(request.imageUrl()); // imageUrl दिला नसेल तर जुनी (अपलोड केलेली) इमेज तशीच राहते
        }

        Product saved = productRepository.save(product);
        if (imageReplaced) {
            imageStorageService.deleteByUrl(oldImage);
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long userId, String role) {
        requireSellerOrAdmin(role);
        Product product = findActive(id);
        requireOwnerOrAdmin(product, userId, role);

        product.setActive(false); // soft delete
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse uploadImage(Long id, Long userId, String role, MultipartFile file) {
        requireSellerOrAdmin(role);
        Product product = findActive(id);
        requireOwnerOrAdmin(product, userId, role);

        String filename = imageStorageService.store(file);
        String newUrl = ImageStorageService.URL_PREFIX + filename;
        String oldUrl = product.getImageUrl();

        try {
            product.setImageUrl(newUrl);
            Product saved = productRepository.save(product);
            imageStorageService.deleteByUrl(oldUrl);
            return toResponse(saved);
        } catch (RuntimeException ex) {
            imageStorageService.deleteByUrl(newUrl); // डेटाबेस सेव्ह अयशस्वी झाले तर नवी फाइल पुसा
            throw ex;
        }
    }

    // ---------- शोध आणि फिल्टर ----------

    private Specification<Product> buildSpec(String search, ProductType type,
                                             BigDecimal minPrice, BigDecimal maxPrice, Long categoryId) {
        final String term = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.<Boolean>get("active")));

            if (term != null) {
                String pattern = "%" + escapeLike(term) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("description")), pattern, '\\')));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sortBy, String direction) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "id"); // डिफॉल्ट: सर्वात नवीन आधी
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid sortBy. Allowed: id, name, price, createdAt");
        }

        Sort.Direction dir;
        if (direction == null || direction.isBlank() || "asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid direction. Allowed: asc, desc");
        }

        if ("id".equals(sortBy)) {
            return Sort.by(dir, "id");
        }
        // दुय्यम sort id ने, म्हणजे पानांमध्ये क्रम स्थिर राहतो
        return Sort.by(new Sort.Order(dir, sortBy), Sort.Order.desc("id"));
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ---------- सहाय्यक मेथड्स ----------

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