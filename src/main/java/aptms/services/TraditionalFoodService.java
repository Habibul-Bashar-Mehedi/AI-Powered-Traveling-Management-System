package aptms.services;

import aptms.entities.TraditionalFood;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TraditionalFoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraditionalFoodService {
    private final TraditionalFoodRepository traditionalFoodRepository;

    public TraditionalFoodService(TraditionalFoodRepository traditionalFoodRepository) {
        this.traditionalFoodRepository = traditionalFoodRepository;
    }

    public TraditionalFood addTraditionalFood(TraditionalFood traditionalFood) {
        if(traditionalFood.getDishName() == null || traditionalFood.getDestination() == null) {
            throw new InvalidException("required dish name and destination");
        }

        boolean exist =
                traditionalFoodRepository
                        .existsByDishNameAndDestinationId(
                          traditionalFood.getDishName(),traditionalFood.getDestination().getId()
                        );
        if(exist) throw new DuplicateValueFoundExceptions("already added");

        return traditionalFoodRepository.save(traditionalFood);
    }

    public List<TraditionalFood> getAllTraditionalFood () {
        return traditionalFoodRepository.findAll();
    }

    //traditional food
    public String deleteTraditionalFood(long id) {
        if(!traditionalFoodRepository.existsById(id)) throw new IdNotFoundException("traditional food id not found");

        traditionalFoodRepository.deleteById(id);
        return "traditional food is deleted";
    }

    public boolean updateTraditionalFood(long id, String dishName,
                                         String description,String culturalContext,
                                         String priceRange,String recommendedLocation) {
        return traditionalFoodRepository.findById(id).map(traditionalFood -> {
            traditionalFood.setDishName(dishName);
            traditionalFood.setDescription(description);
            traditionalFood.setCulturalContext(culturalContext);
            traditionalFood.setPriceRange(priceRange);
            traditionalFood.setRecommendedLocation(recommendedLocation);

            traditionalFoodRepository.save(traditionalFood);

            return true;
        }).orElseThrow(()->
                new IdNotFoundException("traditional food id not found")
        );

    }
}
