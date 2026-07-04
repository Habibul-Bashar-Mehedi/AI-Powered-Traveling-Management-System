package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Market;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.MarketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class MarketService {

    private static final int MAX_LIST_SIZE = 500;

    private final MarketRepository marketRepository;

    public MarketService(MarketRepository marketRepository) {
        this.marketRepository = marketRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Market addMarket(Market market) {
        if(market.getName() == null || market.getDestination() == null) {
            throw new InvalidException(MARKET_NAME_DESTINATION_REQUIRED);
        }

        boolean exist = marketRepository
                .existsMarketsByNameAndDestinationId(
                        market.getName(),
                        market.getDestination().getId());
        if(exist) {
            throw new DuplicateValueFoundExceptions(String.format(DUPLICATE_ENTRY_MESSAGE, MARKET));
        }
        return marketRepository.save(market);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Market> getAllMarket() {
        return marketRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteMarket(long id) {
        if(!marketRepository.existsById(id)) {
            throw new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, MARKET, id));
        }

        marketRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, MARKET);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public void updateMarket(long id, String name, String location,
                             String operatingDays, String operatingHours,
                             String description) {

        marketRepository.findById(id).map(market -> {
            market.setName(name);
            market.setLocation(location);
            market.setOperatingDays(operatingDays);
            market.setOperatingHours(operatingHours);
            market.setDescription(description);

            return marketRepository.save(market);
        }).orElseThrow(() ->
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, MARKET, id))
        );
    }
}
