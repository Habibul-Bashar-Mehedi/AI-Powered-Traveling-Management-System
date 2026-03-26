package aptms.repositories;


import aptms.entities.TraditionalFood;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraditionalFoodRepository extends JpaRepository<TraditionalFood,Long> {
    boolean existsByDishNameAndDestinationId(String dishName, long id);
}
