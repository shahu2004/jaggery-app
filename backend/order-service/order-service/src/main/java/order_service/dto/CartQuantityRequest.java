package order_service.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record CartQuantityRequest(@NotNull(message="Quantity is required") @Min(1) @Max(99) Integer quantity) {}
