package aptms.api;

import aptms.entities.TraditionalFood;
import aptms.services.TraditionalFoodService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/traditional/food")
public class TraditionalFoodRestController {
    private final TraditionalFoodService traditionalFoodService;

    public TraditionalFoodRestController(TraditionalFoodService traditionalFoodService) {
        this.traditionalFoodService = traditionalFoodService;
    }

    @PostMapping("/add")
    public ResponseEntity<TraditionalFood> postTraditionalFood(@RequestBody TraditionalFood traditionalFood) {
        TraditionalFood createdFood = traditionalFoodService.addTraditionalFood(traditionalFood);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFood);
    }

    @GetMapping()
    public ResponseEntity<List<TraditionalFood>> getAllTraditionalFoods(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Long destinationId) {
        List<TraditionalFood> foods = traditionalFoodService.getNearby(lat, lng, radiusKm, destinationId);
        return ResponseEntity.ok(foods);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTraditionalFood(@PathVariable long id, @RequestBody TraditionalFoodUpdateRequest request) {
        traditionalFoodService.updateTraditionalFood(id, request.dishName, request.description,
                request.culturalContext, request.priceRange, request.recommendedLocation, request.linkedServiceId);
        return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, TRADITIONAL_FOOD));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTraditionalFood(@PathVariable long id) {
        String result = traditionalFoodService.deleteTraditionalFood(id);
        return ResponseEntity.ok(result);
    }

    // Inner class for update requests
    public static class TraditionalFoodUpdateRequest {
        public String dishName;
        public String description;
        public String culturalContext;
        public String priceRange;
        public String recommendedLocation;
        public UUID linkedServiceId;
    }
}
