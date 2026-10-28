package order_service.service;

import order_service.dto.CartItemRequest;
import order_service.dto.CartQuantityRequest;
import order_service.dto.ProductSnapshot;
import order_service.entity.CartItem;
import order_service.repository.CartItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartItemRepository cartRepository;
    @Mock ProductClient productClient;
    @InjectMocks CartService service;

    @Test
    void addingAnExistingProductIncreasesItsQuantity() {
        CartItem item = new CartItem();
        item.setUserId(7L);
        item.setProductId(21L);
        item.setQuantity(2);
        when(productClient.getProduct(21L)).thenReturn(new ProductSnapshot(21L, "Jaggery", new BigDecimal("50.00")));
        when(cartRepository.findByUserIdAndProductId(7L, 21L)).thenReturn(Optional.of(item));
        when(cartRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.addItem(7L, new CartItemRequest(21L, 3));

        assertEquals(5, result.quantity());
        verify(cartRepository).save(item);
    }

    @Test
    void quantityLimitIsEnforced() {
        CartItem item = new CartItem();
        item.setUserId(7L);
        item.setProductId(21L);
        item.setQuantity(98);
        when(productClient.getProduct(21L)).thenReturn(new ProductSnapshot(21L, "Jaggery", new BigDecimal("50.00")));
        when(cartRepository.findByUserIdAndProductId(7L, 21L)).thenReturn(Optional.of(item));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addItem(7L, new CartItemRequest(21L, 2)));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode().value());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void quantityCannotBeChangedForAnotherUsersCart() {
        when(productClient.getProduct(21L)).thenReturn(new ProductSnapshot(21L, "Jaggery", new BigDecimal("50.00")));
        when(cartRepository.findByUserIdAndProductId(7L, 21L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.setQuantity(7L, 21L, new CartQuantityRequest(4)));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
    }
}
