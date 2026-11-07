package order_service.dto;
import java.math.BigDecimal;
public record OrderLineResponse(Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
