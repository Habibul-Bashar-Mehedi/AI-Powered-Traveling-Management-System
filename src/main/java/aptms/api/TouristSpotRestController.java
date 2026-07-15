package aptms.api;

import aptms.entities.TouristSpot;
import aptms.services.TouristSpotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/tourist/spot")
public class TouristSpotRestController {
    private final TouristSpotService touristSpotService;

    public TouristSpotRestController(TouristSpotService touristSpotService) {
        this.touristSpotService = touristSpotService;
    }

    @PostMapping("/add")
    public ResponseEntity<TouristSpot> postTouristSpot(@RequestBody TouristSpot touristSpot) {
        TouristSpot createdSpot = touristSpotService.addTouristSpot(touristSpot);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSpot);
    }

    @GetMapping()
    public ResponseEntity<List<TouristSpot>> getAllTouristSpots(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Long destinationId) {
        List<TouristSpot> spots = touristSpotService.getNearby(lat, lng, radiusKm, destinationId);
        return ResponseEntity.ok(spots);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTouristSpot(@PathVariable long id, @RequestBody TouristSpotUpdateRequest request) {
        touristSpotService.updateTouristSpot(id, request.name, request.description,
                request.visitingHours, request.adultEntryFees, request.childEntryFees,
                request.locationDescription, request.requiresTicket, request.linkedServiceId);
        return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, TOURIST_SPOT));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTouristSpot(@PathVariable long id) {
        String result = touristSpotService.deleteTouristSpot(id);
        return ResponseEntity.ok(result);
    }

    // Inner class for update requests
    public static class TouristSpotUpdateRequest {
        public String name;
        public String description;
        public String visitingHours;
        public double adultEntryFees;
        public double childEntryFees;
        public String locationDescription;
        public boolean requiresTicket;
        public UUID linkedServiceId;
    }
}
