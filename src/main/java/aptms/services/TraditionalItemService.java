package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.TraditionalItem;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TraditionalItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class TraditionalItemService {
    private static final int MAX_LIST_SIZE = 500;

    private final TraditionalItemRepository traditionalItemRepository;

    public TraditionalItemService(TraditionalItemRepository traditionalItemRepository) {
        this.traditionalItemRepository = traditionalItemRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public TraditionalItem addTraditionalItem(TraditionalItem traditionalItem) {
        if(traditionalItem.getMarket() == null || traditionalItem.getCategoryName() == null) {
            throw new InvalidException(MARKET_CATEGORY_REQUIRED);
        }

        boolean exist =
                traditionalItemRepository
                        .existsTraditionalItemByMarketIdAndCategoryName(
                          traditionalItem.getMarket().getId(), traditionalItem.getCategoryName()
                        );
        if(exist) {
            throw new DuplicateValueFoundExceptions(String.format(DUPLICATE_ENTRY_MESSAGE, TRADITIONAL_ITEM));
        }

        return traditionalItemRepository.save(traditionalItem);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<TraditionalItem> getAllTraditionalItem() {
        return traditionalItemRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteTraditionalItem(long id) {
        if(!traditionalItemRepository.existsById(id)) {
            throw new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRADITIONAL_ITEM, id));
        }

        traditionalItemRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, TRADITIONAL_ITEM);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public void updateTraditionalItem(long id, String categoryName,
                                      String description, String priceRange) {
        traditionalItemRepository.findById(id).map(traditionalItem -> {
            traditionalItem.setCategoryName(categoryName);
            traditionalItem.setDescription(description);
            traditionalItem.setPriceRange(priceRange);

            return traditionalItemRepository.save(traditionalItem);
        }).orElseThrow(() ->
                new IdNotFoundException(String.format(ENTITY_NOT_FOUND_MESSAGE, TRADITIONAL_ITEM, id))
        );
    }
}
