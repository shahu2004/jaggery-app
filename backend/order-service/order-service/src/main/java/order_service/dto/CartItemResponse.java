package order_service.dto;
import java.time.LocalDateTime;
public record CartItemResponse(Long productId, int quantity, LocalDateTime addedAt) {}
