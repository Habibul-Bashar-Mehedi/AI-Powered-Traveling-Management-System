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

    public String addTraditionalItem(TraditionalItem traditionalItem) {
        if(traditionalItem.getMarket() == null || traditionalItem.getCategoryName() == null) {
            throw new RuntimeException("market and category name required");
        }

        boolean exist =
                traditionalItemRepository
                        .existsTraditionalItemByMarketIdAndCategoryName(
                          traditionalItem.getMarket().getId(),traditionalItem.getCategoryName()
                        );
        if(exist) throw new RuntimeException("already added");

        traditionalItemRepository.save(traditionalItem);

        return "traditional item successfully added";
    }

    public List<TraditionalItem> getAllTraditionalItem () {
        return  traditionalItemRepository.findAll();
    }
}
