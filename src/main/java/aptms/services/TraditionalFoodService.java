package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.TraditionalFood;
import aptms.entities.VendorService;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TraditionalFoodRepository;
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
public class TraditionalFoodService {
    private static final int MAX_LIST_SIZE = 500;

    private final TraditionalFoodRepository traditionalFoodRepository;
    private final DestinationService destinationService;
    private final VendorServiceRepository vendorServiceRepository;

    public TraditionalFoodService(TraditionalFoodRepository traditionalFoodRepository,
                                   DestinationService destinationService,
                                   VendorServiceRepository vendorServiceRepository) {
        this.traditionalFoodRepository = traditionalFoodRepository;
        this.destinationService = destinationService;
        this.vendorServiceRepository = vendorServiceRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public TraditionalFood addTraditionalFood(TraditionalFood traditionalFood) {
        if(traditionalFood.getDishName() == null || traditionalFood.getDestination() == null) {
            throw new InvalidException(DISH_NAME_DESTINATION_REQUIRED);
        }

        boolean exist =
                traditionalFoodRepository
                        .existsByDishNameAndDestinationId(
                          traditionalFood.getDishName(), traditionalFood.getDestination().getId()
                        );
        if(exist) {
            throw new DuplicateValueFoundExceptions(String.format(DUPLICATE_ENTRY_MESSAGE, TRADITIONAL_FOOD));
        }

        return traditionalFoodRepository.save(traditionalFood);
    }

    @Transactional(readOnly = true)
    public List<TraditionalFood> getAllTraditionalFood() {
        return traditionalFoodRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional(readOnly = true)
    public List<TraditionalFood> getNearby(Double lat, Double lng, Double radiusKm, Long destinationId) {
        List<TraditionalFood> all = getAllTraditionalFood();
        if (destinationId != null) {
            return all.stream()
                    .filter(f -> f.getDestination() != null && destinationId.equals(f.getDestination().getId()))
                    .collect(Collectors.toList());
        }
        if (lat != null && lng != null && radiusKm != null) {
            List<Long> orderedIds = destinationService.findNearbyDestinationIds(lat, lng, radiusKm);
            return GeoUtils.orderByDestinationRank(
                    all, f -> f.getDestination() != null ? f.getDestination().getId() : null, orderedIds);
        }
        return all;
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTraditionalFood(long id) {
        if(!traditionalFoodRepository.existsById(id)) {
            throw new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRADITIONAL_FOOD, id));
        }

        traditionalFoodRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, TRADITIONAL_FOOD);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public void updateTraditionalFood(long id, String dishName,
                                      String description, String culturalContext,
                                      String priceRange, String recommendedLocation,
                                      UUID linkedServiceId) {
        traditionalFoodRepository.findById(id).map(traditionalFood -> {
            traditionalFood.setDishName(dishName);
            traditionalFood.setDescription(description);
            traditionalFood.setCulturalContext(culturalContext);
            traditionalFood.setPriceRange(priceRange);
            traditionalFood.setRecommendedLocation(recommendedLocation);
            traditionalFood.setLinkedService(resolveLinkedService(linkedServiceId));

            return traditionalFoodRepository.save(traditionalFood);
        }).orElseThrow(() ->
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRADITIONAL_FOOD, id))
        );
    }

    private VendorService resolveLinkedService(UUID linkedServiceId) {
        if (linkedServiceId == null) return null;
        return vendorServiceRepository.findById(linkedServiceId)
                .orElseThrow(() -> new InvalidException("Linked service not found: " + linkedServiceId));
    }
}
