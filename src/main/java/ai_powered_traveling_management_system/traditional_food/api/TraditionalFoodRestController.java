package ai_powered_traveling_management_system.traditional_food.api;

import ai_powered_traveling_management_system.traditional_food.entities.TraditionalFood;
import ai_powered_traveling_management_system.traditional_food.services.TraditionalFoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traditional/food")
public class TraditionalFoodRestController {
    @Autowired
    private TraditionalFoodService traditionalFoodService;

    @PostMapping("/add")
    public String postTraditionalFood(@RequestBody TraditionalFood traditionalFood) {
        return traditionalFoodService.addTraditionalFood(traditionalFood);
    }
}
