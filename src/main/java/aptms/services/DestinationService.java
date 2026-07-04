package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Destination;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.DestinationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                                     String description) {

        return destinationRepository.findById(id).map(destination -> {
            destination.setName(name);
            destination.setRegion(region);
            destination.setDescription(description);

            destinationRepository.save(destination);
            return true;
        }).orElseThrow(() ->
                new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, DESTINATION, id)
                )
        );
    }
}
