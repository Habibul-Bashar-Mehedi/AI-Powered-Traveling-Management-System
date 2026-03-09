package ai_powered_traveling_management_system.market.services;

import ai_powered_traveling_management_system.market.entities.Market;
import ai_powered_traveling_management_system.market.repositories.MarketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketService {
    @Autowired
    private MarketRepository marketRepository;

    public String addMarket(Market market) {
        if(market.getName() == null || market.getDestination() == null) {
            throw  new RuntimeException("market name and destination required");
        }

        boolean exist = marketRepository
                .existsMarketsByNameAndDestinationId(
                        market.getName(),
                        market.getDestination().getId());

        return "market added successfully done";
    }
}
