package ai_powered_traveling_management_system.tourist_spot.api;

import ai_powered_traveling_management_system.tourist_spot.entities.TouristSpot;
import ai_powered_traveling_management_system.tourist_spot.services.TouristSpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourist/spot")
public class TouristSpotRestController {
    @Autowired
    private TouristSpotService touristSpotService;

    @PostMapping("/add")
    public String postTouristSpot(@RequestBody TouristSpot touristSpot) {
        return touristSpotService.addTouristSpot(touristSpot);
    }
}
