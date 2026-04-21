package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.TraditionalItem;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TraditionalItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TraditionalItemService {
    private final TraditionalItemRepository traditionalItemRepository;

    public TraditionalItemService(TraditionalItemRepository traditionalItemRepository) {
        this.traditionalItemRepository = traditionalItemRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public TraditionalItem addTraditionalItem(TraditionalItem traditionalItem) {
        if(traditionalItem.getMarket() == null || traditionalItem.getCategoryName() == null) {
            throw new InvalidException("market and category name required");
        }

        boolean exist =
                traditionalItemRepository
                        .existsTraditionalItemByMarketIdAndCategoryName(
                          traditionalItem.getMarket().getId(),traditionalItem.getCategoryName()
                        );
        if(exist) throw new DuplicateValueFoundExceptions("already added");

        return traditionalItemRepository.save(traditionalItem);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<TraditionalItem> getAllTraditionalItem () {
        return  traditionalItemRepository.findAll();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTraditionalItem(long id) {
        if(!traditionalItemRepository.existsById(id)) throw new IdNotFoundException("traditional item id not found");

        traditionalItemRepository.deleteById(id);
        return "traditional item is deleted";
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateTraditionalItem(long id,String categoryName,
                                         String description,String priceRange) {
        return traditionalItemRepository.findById(id).map(traditionalItem -> {
            traditionalItem.setCategoryName(categoryName);
            traditionalItem.setDescription(description);
            traditionalItem.setPriceRange(priceRange);

            traditionalItemRepository.save(traditionalItem);

            return true;
        }).orElseThrow(()->
                new IdNotFoundException("traditional item id not found")
        );
    }
}
