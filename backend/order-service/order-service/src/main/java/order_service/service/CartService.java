package order_service.service;
import order_service.dto.CartItemRequest;
import order_service.dto.CartItemResponse;
import order_service.dto.CartQuantityRequest;
import order_service.entity.CartItem;
import order_service.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository carts;
    private final ProductClient products;
    @Transactional(readOnly=true)
    public List<CartItemResponse> getCart(Long userId){
        return carts.findAllByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toResponse).toList();
    }
    @Transactional
    public CartItemResponse addItem(Long userId,CartItemRequest request){
        products.getProduct(request.productId());
        CartItem item=carts.findByUserIdAndProductId(userId,request.productId()).orElseGet(()->{
            CartItem created=new CartItem(); created.setUserId(userId); created.setProductId(request.productId()); created.setQuantity(0); return created;
        });
        int quantity=item.getQuantity()+request.quantity();
        if(quantity>99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cart quantity cannot exceed 99");
        item.setQuantity(quantity);
        return toResponse(carts.save(item));
    }
    @Transactional
    public CartItemResponse setQuantity(Long userId,Long productId,CartQuantityRequest request){
        products.getProduct(productId);
        CartItem item=carts.findByUserIdAndProductId(userId,productId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product is not in the cart"));
        item.setQuantity(request.quantity());
        return toResponse(carts.save(item));
    }
    @Transactional
    public void removeItem(Long userId,Long productId){
        if(carts.findByUserIdAndProductId(userId,productId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product is not in the cart");
        carts.deleteByUserIdAndProductId(userId,productId);
    }
    private CartItemResponse toResponse(CartItem item){return new CartItemResponse(item.getProductId(),item.getQuantity(),item.getCreatedAt());}
}
