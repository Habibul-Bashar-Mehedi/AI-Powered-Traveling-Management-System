package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.TouristSpot;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TouristSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TouristSpotService {
    private final TouristSpotRepository touristSpotRepository;

    public TouristSpotService(TouristSpotRepository touristSpotRepository) {
        this.touristSpotRepository = touristSpotRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public TouristSpot addTouristSpot(TouristSpot touristSpot) {
        if(touristSpot.getDestination() == null || touristSpot.getName() == null) {
            throw new InvalidException("Destination id or spot name required");
        }
        boolean exist = touristSpotRepository
                .existsByNameAndDestinationId(
                        touristSpot.getName(),
                        touristSpot.getDestination().getId());

        if(exist) {
            throw  new DuplicateValueFoundExceptions("Tourist Spot already added");
        }
        return touristSpotRepository.save(touristSpot);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<TouristSpot> getAllTouristSpot() {
        return touristSpotRepository.findAll();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTouristSpot(long id) {
        if(!touristSpotRepository.existsById(id)) throw new IdNotFoundException("tourist spot id not found");

        touristSpotRepository.deleteById(id);
        return "tourist spot is deleted";
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateTouristSpot(long id,String name,
                                     String description,String visitingHours,
                                     double adultEntryFees,double childEntryFees,
                                     String locationDescription) {

        return touristSpotRepository.findById(id).map(touristSpot -> {
            touristSpot.setName(name);
            touristSpot.setDescription(description);
            touristSpot.setVisitingHours(visitingHours);
            touristSpot.setAdultEntryFees(adultEntryFees);
            touristSpot.setChildEntryFees(childEntryFees);
            touristSpot.setLocationDescription(locationDescription);

            touristSpotRepository.save(touristSpot);

            return true;
        }).orElseThrow(()->new IdNotFoundException("tourist spot id not found")
        );

    }
}
