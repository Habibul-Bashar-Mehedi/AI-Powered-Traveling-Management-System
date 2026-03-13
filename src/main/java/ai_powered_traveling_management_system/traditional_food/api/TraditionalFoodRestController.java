package ai_powered_traveling_management_system.traditional_food.api;

import ai_powered_traveling_management_system.traditional_food.entities.TraditionalFood;
import ai_powered_traveling_management_system.traditional_food.services.TraditionalFoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traditional/food")
public class TraditionalFoodRestController {
    @Autowired
    private TraditionalFoodService traditionalFoodService;

    @PostMapping("/add")
    public String postTraditionalFood(@RequestBody TraditionalFood traditionalFood) {
        return traditionalFoodService.addTraditionalFood(traditionalFood);
    }

    @GetMapping("/all")
    public List<TraditionalFood> getAllTraditionalFoods () {
        return traditionalFoodService.getAllTraditionalFood();
    }
}
