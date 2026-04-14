package aptms.api;

import aptms.entities.Destination;
import aptms.services.DestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destination")
public class DestinationRestController {
    @Autowired
    private DestinationService destinationService;

    @PostMapping("/add")
    public Destination postDestination(@RequestBody Destination destination) {
        return destinationService.addDestination(destination);
    }

    @GetMapping()
    public List<Destination> getAllDestination() {
        return destinationService.getAllDestinations();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateDestination(@PathVariable long id, @RequestBody Destination destination) {
        boolean update = destinationService.updateDestination(id,destination.getName(),
                destination.getRegion(), destination.getDescription());

        if(update) {
            return ResponseEntity.ok("destination updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("destination not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDestination(@PathVariable long id) {
        String result = destinationService.deleteDestination(id);
        if(result.equals("destination is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
