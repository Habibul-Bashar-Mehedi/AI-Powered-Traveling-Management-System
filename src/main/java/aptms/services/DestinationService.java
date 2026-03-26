package aptms.services;

import aptms.entities.Destination;
import aptms.repositories.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

}
