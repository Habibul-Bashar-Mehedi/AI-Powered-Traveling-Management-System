package ai_powered_traveling_management_system.destination.api;

import ai_powered_traveling_management_system.destination.entities.Destination;
import ai_powered_traveling_management_system.destination.service.DestinationService;
import jdk.dynalink.linker.LinkerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destination")
public class DestinationRestController {
    @Autowired
    private DestinationService destinationService;

    @PostMapping("/add")
    public String postDestination(@RequestBody Destination destination) {
        return destinationService.addDestination(destination);
    }

    @GetMapping("/all")
    public List<Destination> getAllDestination() {
        return destinationService.getAllDestinations();
    }
}
