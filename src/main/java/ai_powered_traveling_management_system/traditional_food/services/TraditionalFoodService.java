package ai_powered_traveling_management_system.traditional_food.services;

import ai_powered_traveling_management_system.traditional_food.entities.TraditionalFood;
import ai_powered_traveling_management_system.traditional_food.repositories.TraditionalFoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TraditionalFoodService {
    @Autowired
    private TraditionalFoodRepository traditionalFoodRepository;

    public String addTraditionalFood(TraditionalFood traditionalFood) {
        if(traditionalFood.getDishName() == null || traditionalFood.getDestination() == null) {
            throw new RuntimeException("required dish name and destination");
        }

        boolean exist =
                traditionalFoodRepository
                        .existsByDishNameAndDestinationId(
                          traditionalFood.getDishName(),traditionalFood.getDestination().getId()
                        );
        if(exist) throw new RuntimeException("already added");

        traditionalFoodRepository.save(traditionalFood);
        return "traditional food successfully added";
    }
}
