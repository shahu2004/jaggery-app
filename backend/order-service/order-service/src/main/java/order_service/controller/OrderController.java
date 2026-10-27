package order_service.controller;
import order_service.dto.OrderResponse;
import order_service.dto.OrderStatusRequest;
import order_service.security.OrderPrincipal;
import order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;
    @PostMapping("/checkout") public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal OrderPrincipal p){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.checkout(p.userId()));
    }
    @GetMapping public ResponseEntity<List<OrderResponse>> list(@AuthenticationPrincipal OrderPrincipal p){
        return ResponseEntity.ok(service.getOrders(p.userId()));
    }
    @GetMapping("/{id}") public ResponseEntity<OrderResponse> get(@AuthenticationPrincipal OrderPrincipal p,@PathVariable Long id){
        return ResponseEntity.ok(service.getOrder(p.userId(),id));
    }
    @PostMapping("/{id}/cancel") public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal OrderPrincipal p,@PathVariable Long id){
        return ResponseEntity.ok(service.cancelOrder(p.userId(),id));
    }
    @PatchMapping("/{id}/status") public ResponseEntity<OrderResponse> status(@AuthenticationPrincipal OrderPrincipal p,@PathVariable Long id,@Valid @RequestBody OrderStatusRequest request){
        if(!"ADMIN".equals(p.role())) throw new AccessDeniedException("Only admins can change order status");
        return ResponseEntity.ok(service.updateStatus(id,request.status()));
    }
}
