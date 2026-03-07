package ai_powered_traveling_management_system.destination.service;

import ai_powered_traveling_management_system.destination.entities.Destination;
import ai_powered_traveling_management_system.destination.repositories.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DestinationService {
    @Autowired
    private DestinationRepository destinationRepository;

    public String addDestination(Destination destination) {

        if(destination.getName() == null || destination.getRegion() == null) {
            throw new RuntimeException("Name and Region are required!");
        }
        boolean exists = destinationRepository.existsByNameAndRegion(destination.getName(),destination.getRegion());

        if(exists) {
            throw new RuntimeException("this destination already added");
        }

        destinationRepository.save(destination);
        return "destination added successfully done";
    }
}
