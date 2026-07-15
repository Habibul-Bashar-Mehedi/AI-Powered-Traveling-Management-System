package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Destination;
import aptms.entities.TraditionalItem;
import aptms.entities.VendorService;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.DestinationRepository;
import aptms.repositories.TraditionalItemRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.util.GeoUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class TraditionalItemService {
    private static final int MAX_LIST_SIZE = 500;

    private final TraditionalItemRepository traditionalItemRepository;
    private final DestinationRepository destinationRepository;
    private final DestinationService destinationService;
    private final VendorServiceRepository vendorServiceRepository;

    public TraditionalItemService(TraditionalItemRepository traditionalItemRepository,
                                   DestinationRepository destinationRepository,
                                   DestinationService destinationService,
                                   VendorServiceRepository vendorServiceRepository) {
        this.traditionalItemRepository = traditionalItemRepository;
        this.destinationRepository = destinationRepository;
        this.destinationService = destinationService;
        this.vendorServiceRepository = vendorServiceRepository;
    }

    /** item.destination when set directly, else falls back to item.market.destination. */
    private Long effectiveDestinationId(TraditionalItem item) {
        if (item.getDestination() != null) {
            return item.getDestination().getId();
        }
        if (item.getMarket() != null && item.getMarket().getDestination() != null) {
            return item.getMarket().getDestination().getId();
        }
        return null;
    }

    @Transactional
    @SecureAction(role = "USER")
    public TraditionalItem addTraditionalItem(TraditionalItem traditionalItem) {
        if(traditionalItem.getMarket() == null || traditionalItem.getCategoryName() == null) {
            throw new InvalidException(MARKET_CATEGORY_REQUIRED);
        }

        boolean exist =
                traditionalItemRepository
                        .existsTraditionalItemByMarketIdAndCategoryName(
                          traditionalItem.getMarket().getId(), traditionalItem.getCategoryName()
                        );
        if(exist) {
            throw new DuplicateValueFoundExceptions(String.format(DUPLICATE_ENTRY_MESSAGE, TRADITIONAL_ITEM));
        }

        return traditionalItemRepository.save(traditionalItem);
    }

    @Transactional(readOnly = true)
    public List<TraditionalItem> getAllTraditionalItem() {
        return traditionalItemRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional(readOnly = true)
    public List<TraditionalItem> getNearby(Double lat, Double lng, Double radiusKm, Long destinationId) {
        List<TraditionalItem> all = getAllTraditionalItem();
        if (destinationId != null) {
            return all.stream()
                    .filter(i -> destinationId.equals(effectiveDestinationId(i)))
                    .collect(Collectors.toList());
        }
        if (lat != null && lng != null && radiusKm != null) {
            List<Long> orderedIds = destinationService.findNearbyDestinationIds(lat, lng, radiusKm);
            return GeoUtils.orderByDestinationRank(all, this::effectiveDestinationId, orderedIds);
        }
        return all;
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTraditionalItem(long id) {
        if(!traditionalItemRepository.existsById(id)) {
            throw new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRADITIONAL_ITEM, id));
        }

        traditionalItemRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, TRADITIONAL_ITEM);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public void updateTraditionalItem(long id, String categoryName,
                                      String description, String priceRange, Long destinationId,
                                      UUID linkedServiceId) {
        traditionalItemRepository.findById(id).map(traditionalItem -> {
            traditionalItem.setCategoryName(categoryName);
            traditionalItem.setDescription(description);
            traditionalItem.setPriceRange(priceRange);
            traditionalItem.setDestination(resolveDestination(destinationId));
            traditionalItem.setLinkedService(resolveLinkedService(linkedServiceId));

            return traditionalItemRepository.save(traditionalItem);
        }).orElseThrow(() ->
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRADITIONAL_ITEM, id))
        );
    }

    private Destination resolveDestination(Long destinationId) {
        if (destinationId == null) return null;
        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new InvalidException("Destination not found: " + destinationId));
    }

    private VendorService resolveLinkedService(UUID linkedServiceId) {
        if (linkedServiceId == null) return null;
        return vendorServiceRepository.findById(linkedServiceId)
                .orElseThrow(() -> new InvalidException("Linked service not found: " + linkedServiceId));
    }
}
