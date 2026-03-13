package ai_powered_traveling_management_system.traditional_item.api;

import ai_powered_traveling_management_system.traditional_item.entities.TraditionalItem;
import ai_powered_traveling_management_system.traditional_item.service.TraditionalItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traditional/item")
public class TraditionalItemRestController {
    @Autowired
    private TraditionalItemService traditionalItemService;

    @PostMapping("/add")
    public String postTraditionalItem(@RequestBody TraditionalItem traditionalItem) {
        return traditionalItemService.addTraditionalItem(traditionalItem);
    }

    @GetMapping("/all")
    public List<TraditionalItem> getAllTraditionalItems () {
        return traditionalItemService.getAllTraditionalItem();
    }
}
