package order_service.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="customer_orders", indexes=@Index(name="idx_order_user_created", columnList="user_id, created_at"))
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private OrderStatus status;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal totalAmount;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private LocalDateTime updatedAt;
    @OneToMany(mappedBy="purchaseOrder",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("id ASC") private List<OrderLine> items=new ArrayList<>();
    public void addItem(OrderLine item){ item.setPurchaseOrder(this); items.add(item); }
    @PrePersist void onCreate(){ LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
}
