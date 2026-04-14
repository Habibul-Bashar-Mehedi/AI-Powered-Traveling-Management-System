package aptms.services;

import aptms.entities.Transport;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TransportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransportService {
    @Autowired
    private TransportRepository transportRepository;

    public Transport addTransport(Transport transport) {

        if(transport.getOrigin() == null || transport.getDestination() == null) {
            throw new InvalidException("origin id and destination id required");
        }

        boolean exists = transportRepository.existsByOriginIdAndDestinationId(
                transport.getOrigin().getId(),
                transport.getDestination().getId()
        );
        if(exists) {
            throw new DuplicateValueFoundExceptions("already added");
        }

        return transportRepository.save(transport);
    }

    public List<Transport> getAllTransport() {
        return transportRepository.findAll();
    }

    //transport
    public String deleteTransport(long id) {
        if(!transportRepository.existsById(id)) {
            throw new IdNotFoundException("Transport id not found ");
        }
        transportRepository.deleteById(id);
        return "transport is deleted";
    }

    public boolean updateTransport(long id ,String model,
                                   String operatorName ,double estimatedCost,
                                   String estimatedDuration ,String frequency) {
        return transportRepository.findById(id).map(transport -> {
            transport.setModel(model);
            transport.setOperatorName(operatorName);
            transport.setEstimatedCost(estimatedCost);
            transport.setEstimatedDuration(estimatedDuration);
            transport.setFrequency(frequency);

            transportRepository.save(transport);

            return true;
        }).orElseThrow(() ->
                new IdNotFoundException("Transport id not found")
        );
    }
}
