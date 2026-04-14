package aptms.services;

import aptms.entities.Market;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.MarketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketService {
    @Autowired
    private MarketRepository marketRepository;

    public Market addMarket(Market market) {
        if(market.getName() == null || market.getDestination() == null) {
            throw  new InvalidException("market name and destination required");
        }

        boolean exist = marketRepository
                .existsMarketsByNameAndDestinationId(
                        market.getName(),
                        market.getDestination().getId());
        if(exist) {
            throw new DuplicateValueFoundExceptions("already added");
        }
        return marketRepository.save(market);
    }

    public List<Market> getAllMarket() {
        return marketRepository.findAll();
    }

    //market
    public String deleteMarket(long id) {
        if(!marketRepository.existsById(id)) throw new IdNotFoundException("market id not found");

        marketRepository.deleteById(id);
        return "market is deleted";
    }

    public boolean updateMarket(long id ,String name,String location,
                                String operatingDays,String operatingHours,
                                String description) {

        return marketRepository.findById(id).map(market -> {
            market.setName(name);
            market.setLocation(location);
            market.setOperatingDays(operatingDays);
            market.setOperatingHours(operatingHours);
            market.setDescription(description);

            marketRepository.save(market);
            return true;
        }).orElseThrow(()->
                new IdNotFoundException("market id not found")
        );

    }
}
