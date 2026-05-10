package aptms.api;

import aptms.entities.TraditionalItem;
import aptms.services.TraditionalItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/traditional/item")
public class TraditionalItemRestController {
    private final TraditionalItemService traditionalItemService;

    public TraditionalItemRestController(TraditionalItemService traditionalItemService) {
        this.traditionalItemService = traditionalItemService;
    }

    @PostMapping("/add")
    public ResponseEntity<TraditionalItem> postTraditionalItem(@RequestBody TraditionalItem traditionalItem) {
        TraditionalItem createdItem = traditionalItemService.addTraditionalItem(traditionalItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @GetMapping()
    public ResponseEntity<List<TraditionalItem>> getAllTraditionalItems() {
        List<TraditionalItem> items = traditionalItemService.getAllTraditionalItem();
        return ResponseEntity.ok(items);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTraditionalItem(@PathVariable long id, @RequestBody TraditionalItemUpdateRequest request) {
        traditionalItemService.updateTraditionalItem(id, request.categoryName, 
                request.description, request.priceRange);
        return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, TRADITIONAL_ITEM));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTraditionalItem(@PathVariable long id) {
        String result = traditionalItemService.deleteTraditionalItem(id);
        return ResponseEntity.ok(result);
    }

    // Inner class for update requests
    public static class TraditionalItemUpdateRequest {
        public String categoryName;
        public String description;
        public String priceRange;
    }
}
