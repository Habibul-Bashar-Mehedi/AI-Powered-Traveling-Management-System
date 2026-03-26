package aptms.repositories;


import aptms.entities.TraditionalItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraditionalItemRepository extends JpaRepository<TraditionalItem, Long> {
    boolean existsTraditionalItemByMarketIdAndCategoryName(long id, String categoryName);
}
