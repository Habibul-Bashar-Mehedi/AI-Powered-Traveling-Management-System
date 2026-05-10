package aptms.api;

import aptms.entities.Destination;
import aptms.services.DestinationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/destination")
public class DestinationRestController {
    private final DestinationService destinationService;

    public DestinationRestController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @PostMapping("/add")
    public ResponseEntity<Destination> postDestination(@RequestBody Destination destination) {
        Destination created = destinationService.addDestination(destination);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping()
    public ResponseEntity<List<Destination>> getAllDestination() {
        List<Destination> destinations = destinationService.getAllDestinations();
        return ResponseEntity.ok(destinations);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateDestination(@PathVariable long id, @RequestBody Destination destination) {
        boolean updated = destinationService.updateDestination(
            id,
            destination.getName(),
            destination.getRegion(), 
            destination.getDescription()
        );

        if(updated) {
            return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, DESTINATION));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format(ENTITY_NOT_FOUND_MESSAGE, DESTINATION, id));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDestination(@PathVariable long id) {
        String result = destinationService.deleteDestination(id);
        return ResponseEntity.ok(result);
    }
}
