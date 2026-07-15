package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.TouristSpot;
import aptms.entities.VendorService;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TouristSpotRepository;
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
public class TouristSpotService {
    private static final int MAX_LIST_SIZE = 500;

    private final TouristSpotRepository touristSpotRepository;
    private final DestinationService destinationService;
    private final VendorServiceRepository vendorServiceRepository;

    public TouristSpotService(TouristSpotRepository touristSpotRepository,
                               DestinationService destinationService,
                               VendorServiceRepository vendorServiceRepository) {
        this.touristSpotRepository = touristSpotRepository;
        this.destinationService = destinationService;
        this.vendorServiceRepository = vendorServiceRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public TouristSpot addTouristSpot(TouristSpot touristSpot) {
        if(touristSpot.getDestination() == null || touristSpot.getName() == null) {
            throw new InvalidException(DESTINATION_NAME_REQUIRED);
        }
        boolean exist = touristSpotRepository
                .existsByNameAndDestinationId(
                        touristSpot.getName(),
                        touristSpot.getDestination().getId());

        if(exist) {
            throw new DuplicateValueFoundExceptions(String.format(DUPLICATE_ENTRY_MESSAGE, TOURIST_SPOT));
        }
        return touristSpotRepository.save(touristSpot);
    }

    @Transactional(readOnly = true)
    public List<TouristSpot> getAllTouristSpot() {
        return touristSpotRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    /**
     * Filters/sorts tourist spots by destinationId (exact match) or by lat/lng/radiusKm
     * (nearest destination first). Falls back to getAllTouristSpot() when none are given —
     * fully backward compatible with the plain listing used today.
     */
    @Transactional(readOnly = true)
    public List<TouristSpot> getNearby(Double lat, Double lng, Double radiusKm, Long destinationId) {
        List<TouristSpot> all = getAllTouristSpot();
        if (destinationId != null) {
            return all.stream()
                    .filter(s -> s.getDestination() != null && destinationId.equals(s.getDestination().getId()))
                    .collect(Collectors.toList());
        }
        if (lat != null && lng != null && radiusKm != null) {
            List<Long> orderedIds = destinationService.findNearbyDestinationIds(lat, lng, radiusKm);
            return GeoUtils.orderByDestinationRank(
                    all, s -> s.getDestination() != null ? s.getDestination().getId() : null, orderedIds);
        }
        return all;
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTouristSpot(long id) {
        if(!touristSpotRepository.existsById(id)) {
            throw new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TOURIST_SPOT, id));
        }

        touristSpotRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, TOURIST_SPOT);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public void updateTouristSpot(long id, String name,
                                  String description, String visitingHours,
                                  double adultEntryFees, double childEntryFees,
                                  String locationDescription,
                                  boolean requiresTicket, UUID linkedServiceId) {

        touristSpotRepository.findById(id).map(touristSpot -> {
            touristSpot.setName(name);
            touristSpot.setDescription(description);
            touristSpot.setVisitingHours(visitingHours);
            touristSpot.setAdultEntryFees(adultEntryFees);
            touristSpot.setChildEntryFees(childEntryFees);
            touristSpot.setLocationDescription(locationDescription);
            touristSpot.setRequiresTicket(requiresTicket);
            touristSpot.setLinkedService(resolveLinkedService(linkedServiceId));

            return touristSpotRepository.save(touristSpot);
        }).orElseThrow(() ->
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TOURIST_SPOT, id))
        );
    }

    private VendorService resolveLinkedService(UUID linkedServiceId) {
        if (linkedServiceId == null) return null;
        return vendorServiceRepository.findById(linkedServiceId)
                .orElseThrow(() -> new InvalidException("Linked service not found: " + linkedServiceId));
    }
}
