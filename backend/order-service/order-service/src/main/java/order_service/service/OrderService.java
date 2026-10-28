package order_service.service;
import order_service.dto.AdminOrderResponse;
import order_service.dto.OrderResponse;
import order_service.dto.ProductSnapshot;
import order_service.entity.CartItem;
import order_service.entity.OrderLine;
import order_service.entity.OrderStatus;
import order_service.entity.PurchaseOrder;
import order_service.repository.CartItemRepository;
import order_service.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartItemRepository carts;
    private final PurchaseOrderRepository orders;
    private final ProductClient products;
    @Transactional
    public OrderResponse checkout(Long userId){
        List<CartItem> cart=carts.findAllByUserIdOrderByCreatedAtAsc(userId);
        if(cart.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cart is empty");
        PurchaseOrder order=new PurchaseOrder(); order.setUserId(userId); order.setStatus(OrderStatus.PLACED);
        BigDecimal total=BigDecimal.ZERO;
        for(CartItem ci:cart){
            ProductSnapshot p=products.getProduct(ci.getProductId());
            if(p.price().signum()<=0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Product has invalid price");
            BigDecimal lineTotal=p.price().multiply(BigDecimal.valueOf(ci.getQuantity()));
            OrderLine line=new OrderLine(); line.setProductId(p.id()); line.setProductName(p.name());
            line.setUnitPrice(p.price()); line.setQuantity(ci.getQuantity()); line.setLineTotal(lineTotal);
            order.addItem(line); total=total.add(lineTotal);
        }
        order.setTotalAmount(total);
        PurchaseOrder saved=orders.save(order);
        carts.deleteAllByUserId(userId);
        return OrderResponse.from(saved);
    }
    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getAllOrdersForAdmin() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(AdminOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly=true)
    public List<OrderResponse> getOrders(Long userId){
        return orders.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }
    @Transactional(readOnly=true)
    public OrderResponse getOrder(Long userId,Long id){
        PurchaseOrder o=orders.findByIdAndUserId(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));
        return OrderResponse.from(o);
    }
    @Transactional
    public OrderResponse cancelOrder(Long userId,Long id){
        PurchaseOrder o=orders.findByIdAndUserId(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));
        if(o.getStatus()!=OrderStatus.PLACED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only PLACED orders can be cancelled");
        o.setStatus(OrderStatus.CANCELLED); return OrderResponse.from(orders.save(o));
    }
    @Transactional
    public OrderResponse updateStatus(Long id,OrderStatus requested){
        PurchaseOrder o=orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));
        boolean allowed=switch(o.getStatus()){
            case PLACED -> requested==OrderStatus.CONFIRMED||requested==OrderStatus.CANCELLED;
            case CONFIRMED -> requested==OrderStatus.SHIPPED||requested==OrderStatus.CANCELLED;
            case SHIPPED -> requested==OrderStatus.DELIVERED;
            case DELIVERED,CANCELLED -> false;
        };
        if(!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid order status transition");
        o.setStatus(requested); return OrderResponse.from(orders.save(o));
    }
}
