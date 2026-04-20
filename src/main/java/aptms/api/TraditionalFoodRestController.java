package aptms.api;

import aptms.entities.TraditionalFood;
import aptms.services.TraditionalFoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traditional/food")
public class TraditionalFoodRestController {
    private final TraditionalFoodService traditionalFoodService;

    public TraditionalFoodRestController(TraditionalFoodService traditionalFoodService) {
        this.traditionalFoodService = traditionalFoodService;
    }

    @PostMapping("/add")
    public TraditionalFood postTraditionalFood(@RequestBody TraditionalFood traditionalFood) {
        return traditionalFoodService.addTraditionalFood(traditionalFood);
    }

    @GetMapping()
    public List<TraditionalFood> getAllTraditionalFoods () {
        return traditionalFoodService.getAllTraditionalFood();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTraditionalFood(@PathVariable long id, @RequestBody TraditionalFood traditionalFood) {

        boolean update = traditionalFoodService.updateTraditionalFood(id,traditionalFood.getDishName(),
                traditionalFood.getDescription(), traditionalFood.getCulturalContext(),
                traditionalFood.getPriceRange(), traditionalFood.getRecommendedLocation());

        if(update) {
            return ResponseEntity.ok("traditional food updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("traditional food not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTraditionalFood(@PathVariable long id) {
        String result = traditionalFoodService.deleteTraditionalFood(id);
        if(result.equals("traditional food is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
