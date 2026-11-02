package order_service.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
@Entity
@Table(name="order_lines", indexes=@Index(name="idx_order_line_order", columnList="order_id"))
@Getter @Setter @NoArgsConstructor
public class OrderLine {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false)
    private PurchaseOrder purchaseOrder;
    @Column(name="product_id",nullable=false) private Long productId;
    @Column(name="product_name",nullable=false,length=150) private String productName;
    @Column(name="unit_price",nullable=false,precision=10,scale=2) private BigDecimal unitPrice;
    @Column(nullable=false) private int quantity;
    @Column(name="line_total",nullable=false,precision=12,scale=2) private BigDecimal lineTotal;
}
