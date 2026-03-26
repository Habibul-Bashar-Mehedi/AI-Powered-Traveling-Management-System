package aptms.repositories;

import aptms.entities.Market;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market,Long> {
    boolean existsMarketsByNameAndDestinationId(String name, long id);
}
