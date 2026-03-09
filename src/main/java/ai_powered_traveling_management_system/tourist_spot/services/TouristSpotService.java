package ai_powered_traveling_management_system.tourist_spot.services;

import ai_powered_traveling_management_system.tourist_spot.entities.TouristSpot;
import ai_powered_traveling_management_system.tourist_spot.repositories.TouristSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TouristSpotService {
    @Autowired
    private TouristSpotRepository touristSpotRepository;

    public String addTouristSpot(TouristSpot touristSpot) {
        if(touristSpot.getDestination() == null || touristSpot.getName() == null) {
            throw new RuntimeException("Destination id or spot name required");
        }
        boolean exist = touristSpotRepository
                .existsByNameAndDestinationId(
                        touristSpot.getName(),
                        touristSpot.getDestination().getId());

        if(exist) {
            throw  new RuntimeException("Tourist Spot already added");
        }
        touristSpotRepository.save(touristSpot);
        return "Tourist Spot Successfully Added";
    }
}