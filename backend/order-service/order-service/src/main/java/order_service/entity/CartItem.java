package order_service.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name="cart_items", uniqueConstraints=@UniqueConstraint(name="uk_cart_user_product", columnNames={"user_id","product_id"}),
       indexes=@Index(name="idx_cart_user", columnList="user_id"))
@Getter @Setter @NoArgsConstructor
public class CartItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="product_id", nullable=false) private Long productId;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    @PrePersist void onCreate(){ LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
}
