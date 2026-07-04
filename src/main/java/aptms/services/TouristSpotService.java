package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.TouristSpot;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TouristSpotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class TouristSpotService {
    private static final int MAX_LIST_SIZE = 500;

    private final TouristSpotRepository touristSpotRepository;

    public TouristSpotService(TouristSpotRepository touristSpotRepository) {
        this.touristSpotRepository = touristSpotRepository;
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
    @SecureAction(role = "ADMIN")
    public List<TouristSpot> getAllTouristSpot() {
        return touristSpotRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
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
                                  String locationDescription) {

        touristSpotRepository.findById(id).map(touristSpot -> {
            touristSpot.setName(name);
            touristSpot.setDescription(description);
            touristSpot.setVisitingHours(visitingHours);
            touristSpot.setAdultEntryFees(adultEntryFees);
            touristSpot.setChildEntryFees(childEntryFees);
            touristSpot.setLocationDescription(locationDescription);

            return touristSpotRepository.save(touristSpot);
        }).orElseThrow(() -> 
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TOURIST_SPOT, id))
        );
    }
}
