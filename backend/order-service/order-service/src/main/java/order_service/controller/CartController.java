package order_service.controller;
import order_service.dto.CartItemRequest;
import order_service.dto.CartItemResponse;
import order_service.dto.CartQuantityRequest;
import order_service.security.OrderPrincipal;
import order_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    @GetMapping public ResponseEntity<List<CartItemResponse>> getCart(@AuthenticationPrincipal OrderPrincipal p){
        return ResponseEntity.ok(cartService.getCart(p.userId()));
    }
    @PostMapping("/items") public ResponseEntity<CartItemResponse> add(@AuthenticationPrincipal OrderPrincipal p,@Valid @RequestBody CartItemRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(p.userId(),request));
    }
    @PutMapping("/items/{productId}") public ResponseEntity<CartItemResponse> setQuantity(@AuthenticationPrincipal OrderPrincipal p,@PathVariable Long productId,@Valid @RequestBody CartQuantityRequest request){
        return ResponseEntity.ok(cartService.setQuantity(p.userId(),productId,request));
    }
    @DeleteMapping("/items/{productId}") public ResponseEntity<Void> remove(@AuthenticationPrincipal OrderPrincipal p,@PathVariable Long productId){
        cartService.removeItem(p.userId(),productId); return ResponseEntity.noContent().build();
    }
}
