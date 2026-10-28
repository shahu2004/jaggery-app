package order_service.repository;
import order_service.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long> {
    List<PurchaseOrder> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<PurchaseOrder> findByIdAndUserId(Long id, Long userId);
}
