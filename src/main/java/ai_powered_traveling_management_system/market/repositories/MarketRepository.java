package ai_powered_traveling_management_system.market.repositories;

import ai_powered_traveling_management_system.market.entities.Market;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market,Long> {
    boolean existsMarketsByNameAndDestinationId(String name, long id);
}
