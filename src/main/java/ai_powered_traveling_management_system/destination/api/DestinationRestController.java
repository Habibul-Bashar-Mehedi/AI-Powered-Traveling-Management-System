package ai_powered_traveling_management_system.destination.api;

import ai_powered_traveling_management_system.destination.entities.Destination;
import ai_powered_traveling_management_system.destination.service.DestinationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/desination")
public class DestinationRestController {
    @Autowired
    private DestinationService destinationService;

    @PostMapping("/add")
    public String postDestination(@RequestBody Destination destination) {
        return destinationService.addDestination(destination);
    }
}
