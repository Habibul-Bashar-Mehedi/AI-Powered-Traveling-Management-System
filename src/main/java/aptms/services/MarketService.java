package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Market;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.MarketRepository;
import aptms.util.GeoUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class MarketService {

    private static final int MAX_LIST_SIZE = 500;

    private final MarketRepository marketRepository;
    private final DestinationService destinationService;

    public MarketService(MarketRepository marketRepository, DestinationService destinationService) {
        this.marketRepository = marketRepository;
        this.destinationService = destinationService;
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
    public List<Market> getAllMarket() {
        return marketRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional(readOnly = true)
    public List<Market> getNearby(Double lat, Double lng, Double radiusKm, Long destinationId) {
        List<Market> all = getAllMarket();
        if (destinationId != null) {
            return all.stream()
                    .filter(m -> m.getDestination() != null && destinationId.equals(m.getDestination().getId()))
                    .collect(Collectors.toList());
        }
        if (lat != null && lng != null && radiusKm != null) {
            List<Long> orderedIds = destinationService.findNearbyDestinationIds(lat, lng, radiusKm);
            return GeoUtils.orderByDestinationRank(
                    all, m -> m.getDestination() != null ? m.getDestination().getId() : null, orderedIds);
        }
        return all;
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
