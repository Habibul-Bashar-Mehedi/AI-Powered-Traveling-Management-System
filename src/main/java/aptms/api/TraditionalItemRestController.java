package aptms.api;

import aptms.entities.TraditionalItem;
import aptms.services.TraditionalItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traditional/item")
public class TraditionalItemRestController {
    @Autowired
    private TraditionalItemService traditionalItemService;

    @PostMapping("/add")
    public TraditionalItem postTraditionalItem(@RequestBody TraditionalItem traditionalItem) {
        return traditionalItemService.addTraditionalItem(traditionalItem);
    }

    @GetMapping()
    public List<TraditionalItem> getAllTraditionalItems () {
        return traditionalItemService.getAllTraditionalItem();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTraditionalItem(@PathVariable long id, @RequestBody TraditionalItem traditionalItem) {
        boolean update = traditionalItemService.updateTraditionalItem(id,traditionalItem.getCategoryName(),traditionalItem.getDescription(),traditionalItem.getPriceRange());

        if(update) {
            return ResponseEntity.ok("traditional item updated");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("traditional item not found with id: "+id);
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTraditionalItem(@PathVariable long id) {
        String result = traditionalItemService.deleteTraditionalItem(id);
        if(result.equals("traditional item is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
