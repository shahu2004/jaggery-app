package order_service.dto;
import order_service.entity.OrderStatus;
import order_service.entity.PurchaseOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt, List<OrderLineResponse> items) {
    public static OrderResponse from(PurchaseOrder order) {
        List<OrderLineResponse> lines=order.getItems().stream().map(i->new OrderLineResponse(
            i.getProductId(),i.getProductName(),i.getUnitPrice(),i.getQuantity(),i.getLineTotal())).toList();
        return new OrderResponse(order.getId(),order.getStatus(),order.getTotalAmount(),order.getCreatedAt(),lines);
    }
}
