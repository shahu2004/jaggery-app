package order_service.service;

import order_service.dto.OrderResponse;
import order_service.dto.ProductSnapshot;
import order_service.entity.CartItem;
import order_service.entity.OrderStatus;
import order_service.entity.PurchaseOrder;
import order_service.repository.CartItemRepository;
import order_service.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock CartItemRepository cartRepository;
    @Mock PurchaseOrderRepository orderRepository;
    @Mock ProductClient productClient;
    @InjectMocks OrderService service;

    @Test
    void checkoutUsesServerPricesCreatesSnapshotAndClearsCart() {
        CartItem first = cart(21L, 2);
        CartItem second = cart(22L, 3);
        when(cartRepository.findAllByUserIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(first, second));
        when(productClient.getProduct(21L)).thenReturn(new ProductSnapshot(21L, "Jaggery Block", new BigDecimal("100.00")));
        when(productClient.getProduct(22L)).thenReturn(new ProductSnapshot(22L, "Jaggery Powder", new BigDecimal("20.00")));
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.checkout(7L);

        assertEquals(OrderStatus.PLACED, response.status());
        assertEquals(new BigDecimal("260.00"), response.totalAmount());
        assertEquals(2, response.items().size());
        assertEquals("Jaggery Block", response.items().get(0).productName());
        assertEquals(new BigDecimal("100.00"), response.items().get(0).unitPrice());
        verify(cartRepository).deleteAllByUserId(7L);
        verify(orderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void checkoutRejectsAnEmptyCart() {
        when(cartRepository.findAllByUserIdOrderByCreatedAtAsc(8L)).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.checkout(8L));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode().value());
        verifyNoInteractions(productClient);
        verify(cartRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void orderCannotBeReadByAnotherCustomer() {
        when(orderRepository.findByIdAndUserId(90L, 7L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.getOrder(7L, 90L));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
    }

    @Test
    void deliveredOrderCannotBeReopened() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(5L);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.updateStatus(5L, OrderStatus.CONFIRMED));

        assertEquals(HttpStatus.CONFLICT.value(), exception.getStatusCode().value());
        verify(orderRepository, never()).save(any());
    }

    private static CartItem cart(Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setUserId(7L);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
