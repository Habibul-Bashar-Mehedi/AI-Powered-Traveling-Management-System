package aptms.services;

import aptms.entities.TraditionalItem;
import aptms.repositories.TraditionalItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraditionalItemService {
    @Autowired
    private TraditionalItemRepository traditionalItemRepository;

    public TraditionalItem addTraditionalItem(TraditionalItem traditionalItem) {
        if(traditionalItem.getMarket() == null || traditionalItem.getCategoryName() == null) {
            throw new RuntimeException("market and category name required");
        }

        boolean exist =
                traditionalItemRepository
                        .existsTraditionalItemByMarketIdAndCategoryName(
                          traditionalItem.getMarket().getId(),traditionalItem.getCategoryName()
                        );
        if(exist) throw new RuntimeException("already added");

        return traditionalItemRepository.save(traditionalItem);
    }

    public List<TraditionalItem> getAllTraditionalItem () {
        return  traditionalItemRepository.findAll();
    }

    //traditional item
    public String deleteTraditionalItem(long id) {
        if(!traditionalItemRepository.existsById(id)) return "traditional item not found";

        traditionalItemRepository.deleteById(id);
        return "traditional item is deleted";
    }

    public boolean updateTraditionalItem(long id,String categoryName,
                                         String description,String priceRange) {
        return traditionalItemRepository.findById(id).map(traditionalItem -> {
            traditionalItem.setCategoryName(categoryName);
            traditionalItem.setDescription(description);
            traditionalItem.setPriceRange(priceRange);

            traditionalItemRepository.save(traditionalItem);

            return true;
        }).orElse(false);
    }
}
