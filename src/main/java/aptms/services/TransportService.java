package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Transport;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TransportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class TransportService {
    private static final int MAX_LIST_SIZE = 500;

    private final TransportRepository transportRepository;

    public TransportService(TransportRepository transportRepository) {
        this.transportRepository = transportRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Transport addTransport(Transport transport) {

        if(transport.getOrigin() == null || transport.getDestination() == null) {
            throw new InvalidException(ORIGIN_DESTINATION_REQUIRED);
        }

        boolean exists = transportRepository.existsByOriginIdAndDestinationId(
                transport.getOrigin().getId(),
                transport.getDestination().getId()
        );
        if(exists) {
            throw new DuplicateValueFoundExceptions(String.format(DUPLICATE_ENTRY_MESSAGE, TRANSPORT));
        }

        return transportRepository.save(transport);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Transport> getAllTransport() {
        return transportRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTransport(long id) {
        if(!transportRepository.existsById(id)) {
            throw new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRANSPORT, id));
        }
        transportRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, TRANSPORT);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public void updateTransport(long id, String model,
                                String operatorName, double estimatedCost,
                                String estimatedDuration, String frequency) {
        transportRepository.findById(id).map(transport -> {
            transport.setModel(model);
            transport.setOperatorName(operatorName);
            transport.setEstimatedCost(estimatedCost);
            transport.setEstimatedDuration(estimatedDuration);
            transport.setFrequency(frequency);

            return transportRepository.save(transport);
        }).orElseThrow(() ->
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRANSPORT, id))
        );
    }
}
