package aptms.api;

import aptms.entities.Market;
import aptms.services.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketRestController {
    private final MarketService marketService;

    public MarketRestController(MarketService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/add")
    public Market postMarket(@RequestBody Market market) {
        return marketService.addMarket(market);
    }

    @GetMapping()
    public List<Market> getAllMarket() {
        return marketService.getAllMarket();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateMarket(@PathVariable long id, @RequestBody Market market) {
        boolean update = marketService.updateMarket(id,market.getName(), market.getLocation(),
                market.getOperatingDays(), market.getOperatingHours(), market.getDescription());

        if(update) {
            return ResponseEntity.ok("market updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("market not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMarket(@PathVariable long id) {
        String result = marketService.deleteMarket(id);
        if(result.equals("market is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
