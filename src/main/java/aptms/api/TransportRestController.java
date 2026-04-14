package aptms.api;

import aptms.entities.Transport;
import aptms.services.TransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transports")
public class TransportRestController {
    @Autowired
    private TransportService transportService;

    @PostMapping("/add")
    public Transport postTransport(@RequestBody Transport transport) {
        return transportService.addTransport(transport);
    }

    @GetMapping()
    public List<Transport> getAllTransports () {
        return transportService.getAllTransport();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTransport(@PathVariable long id , @RequestBody Transport transport){
        boolean update = transportService.updateTransport(id,transport.getModel(),transport.getOperatorName(),transport.getEstimatedCost(),
                transport.getEstimatedDuration(),transport.getFrequency());

        if (update) {
            return ResponseEntity.ok("transport updated successfully done");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("transport id not found with id: "+id);
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTransport(@PathVariable long id) {
        String result = transportService.deleteTransport(id);
        if(result.equals("transport is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
