package ai_powered_traveling_management_system.traditional_food.repositories;


import ai_powered_traveling_management_system.traditional_food.entities.TraditionalFood;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraditionalFoodRepository extends JpaRepository<TraditionalFood,Long> {
    boolean existsByDishNameAndDestinationId(String dishName, long id);
}
