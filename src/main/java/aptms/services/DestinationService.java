package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Destination;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.DestinationRepository;
import aptms.util.GeoUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class DestinationService {
    private static final int MAX_LIST_SIZE = 500;

    private final DestinationRepository destinationRepository;

    public DestinationService(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Destination addDestination(Destination destination) {

        if(destination.getName() == null || destination.getRegion() == null) {
            throw new InvalidException(
                String.format(REQUIRED_FIELD_MESSAGE, FIELD_NAME + " and " + FIELD_REGION)
            );
        }
        
        boolean exists = destinationRepository.existsByNameAndRegion(
            destination.getName(), 
            destination.getRegion()
        );

        if(exists) {
            throw new DuplicateValueFoundExceptions(
                String.format(ENTITY_ALREADY_EXISTS_MESSAGE, DESTINATION)
            );
        }

        return destinationRepository.save(destination);
    }

    @Transactional(readOnly = true)
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    /**
     * Destinations with coordinates within radiusKm of (lat, lng), nearest first.
     * Falls back to the unfiltered list when any of the three params is missing.
     */
    @Transactional(readOnly = true)
    public List<Destination> findNearby(Double lat, Double lng, Double radiusKm) {
        List<Destination> all = getAllDestinations();
        if (lat == null || lng == null || radiusKm == null) {
            return all;
        }
        return all.stream()
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .filter(d -> GeoUtils.distanceKm(lat, lng, d.getLatitude(), d.getLongitude()) <= radiusKm)
                .sorted((a, b) -> Double.compare(
                        GeoUtils.distanceKm(lat, lng, a.getLatitude(), a.getLongitude()),
                        GeoUtils.distanceKm(lat, lng, b.getLatitude(), b.getLongitude())))
                .collect(Collectors.toList());
    }

    /** Same as findNearby, but just the ids, in distance order — for filtering related content. */
    @Transactional(readOnly = true)
    public List<Long> findNearbyDestinationIds(Double lat, Double lng, Double radiusKm) {
        return findNearby(lat, lng, radiusKm).stream().map(Destination::getId).collect(Collectors.toList());
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteDestination(long id) {
        if(!destinationRepository.existsById(id)) {
            throw new IdNotFoundException(
                String.format(ENTITY_NOT_FOUND_MESSAGE, DESTINATION, id)
            );
        }

        destinationRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, DESTINATION);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateDestination(long id, String name, String region,
                                     String description, Double latitude, Double longitude) {

        return destinationRepository.findById(id).map(destination -> {
            destination.setName(name);
            destination.setRegion(region);
            destination.setDescription(description);
            destination.setLatitude(latitude);
            destination.setLongitude(longitude);

            destinationRepository.save(destination);
            return true;
        }).orElseThrow(() ->
                new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, DESTINATION, id)
                )
        );
    }
}
