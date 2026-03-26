package aptms.services;

import aptms.entities.Market;
import aptms.repositories.MarketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        marketRepository.save(market);
        return "market added successfully done";
    }

    public List<Market> getAllMarket() {
        return marketRepository.findAll();
    }
}
