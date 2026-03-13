package ai_powered_traveling_management_system.transport.service;

import ai_powered_traveling_management_system.transport.entities.Transport;
import ai_powered_traveling_management_system.transport.repository.TransportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransportService {
    @Autowired
    private TransportRepository transportRepository;

    public String addTransport(Transport transport) {

        if(transport.getOrigin() == null || transport.getDestination() == null) {
            throw new RuntimeException("origin id and destination id required");
        }

        boolean exists = transportRepository.existsByOriginIdAndDestinationId(
                transport.getOrigin().getId(),
                transport.getDestination().getId()
        );
        if(exists) {
            throw new RuntimeException("already added");
        }

        transportRepository.save(transport);
        return "transport successfully done";
    }

    public List<Transport> getAllTransport() {
        return transportRepository.findAll();
    }
}
