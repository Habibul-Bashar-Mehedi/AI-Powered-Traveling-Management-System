package aptms.services;

import aptms.entities.Destination;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
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
            throw new InvalidException("Name and Region are required!");
        }
        boolean exists = destinationRepository.existsByNameAndRegion(destination.getName(),destination.getRegion());

        if(exists) {
            throw new DuplicateValueFoundExceptions("this destination already added");
        }

        return destinationRepository.save(destination);
    }
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    //destination
    public String deleteDestination(long id) {
        if(!destinationRepository.existsById(id)) throw new IdNotFoundException("destination id not found");

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
        }).orElseThrow(()->
                new IdNotFoundException("destination id not found")
        );
    }
}
