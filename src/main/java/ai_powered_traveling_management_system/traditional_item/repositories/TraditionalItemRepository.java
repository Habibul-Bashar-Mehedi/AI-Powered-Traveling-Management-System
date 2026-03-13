package ai_powered_traveling_management_system.traditional_item.repositories;


import ai_powered_traveling_management_system.traditional_item.entities.TraditionalItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraditionalItemRepository extends JpaRepository<TraditionalItem, Long> {
    boolean existsTraditionalItemByMarketIdAndCategoryName(long id, String categoryName);
}
