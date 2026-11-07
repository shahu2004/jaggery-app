package order_service.dto;

import order_service.entity.OrderStatus;
import order_service.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderLineResponse> items) {

    public static AdminOrderResponse from(PurchaseOrder order) {
        List<OrderLineResponse> lines = order.getItems().stream()
                .map(item -> new OrderLineResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();

        return new AdminOrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                lines);
    }
}
