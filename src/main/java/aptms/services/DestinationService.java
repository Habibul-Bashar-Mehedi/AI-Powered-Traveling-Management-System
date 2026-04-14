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

    public Destination addDestination(Destination destination) {

        if(destination.getName() == null || destination.getRegion() == null) {
            throw new RuntimeException("Name and Region are required!");
        }
        boolean exists = destinationRepository.existsByNameAndRegion(destination.getName(),destination.getRegion());

        if(exists) {
            throw new RuntimeException("this destination already added");
        }

        return destinationRepository.save(destination);
    }
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    //destination
    public String deleteDestination(long id) {
        if(!destinationRepository.existsById(id)) return "destination not found";

        destinationRepository.deleteById(id);
        return "destination is deleted";
    }

    public boolean updateDestination(long id,String name, String region,
                                     String description) {

        return destinationRepository.findById(id).map(destination -> {
            destination.setName(name);
            destination.setRegion(region);
            destination.setDescription(description);

            destinationRepository.save(destination);
            return true;
        }).orElse(false);
    }
}
