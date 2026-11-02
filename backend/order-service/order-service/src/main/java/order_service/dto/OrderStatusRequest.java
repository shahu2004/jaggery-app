package order_service.dto;
import jakarta.validation.constraints.NotNull;
import order_service.entity.OrderStatus;
public record OrderStatusRequest(@NotNull(message="Order status is required") OrderStatus status) {}
