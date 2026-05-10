package aptms.api;

import aptms.entities.Transport;
import aptms.services.TransportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/transports")
public class TransportRestController {
    private final TransportService transportService;

    public TransportRestController(TransportService transportService) {
        this.transportService = transportService;
    }

    @PostMapping("/add")
    public ResponseEntity<Transport> postTransport(@RequestBody Transport transport) {
        Transport createdTransport = transportService.addTransport(transport);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransport);
    }

    @GetMapping()
    public ResponseEntity<List<Transport>> getAllTransports() {
        List<Transport> transports = transportService.getAllTransport();
        return ResponseEntity.ok(transports);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTransport(@PathVariable long id, @RequestBody TransportUpdateRequest request) {
        transportService.updateTransport(id, request.model, request.operatorName, 
                request.estimatedCost, request.estimatedDuration, request.frequency);
        return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, TRANSPORT));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTransport(@PathVariable long id) {
        String result = transportService.deleteTransport(id);
        return ResponseEntity.ok(result);
    }

    // Inner class for update requests
    public static class TransportUpdateRequest {
        public String model;
        public String operatorName;
        public double estimatedCost;
        public String estimatedDuration;
        public String frequency;
    }
}
