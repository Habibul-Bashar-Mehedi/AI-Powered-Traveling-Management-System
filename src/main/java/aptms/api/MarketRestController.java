package aptms.api;

import aptms.entities.Market;
import aptms.services.MarketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/market")
public class MarketRestController {
    private final MarketService marketService;

    public MarketRestController(MarketService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/add")
    public ResponseEntity<Market> postMarket(@RequestBody Market market) {
        Market createdMarket = marketService.addMarket(market);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMarket);
    }

    @GetMapping()
    public ResponseEntity<List<Market>> getAllMarket(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Long destinationId) {
        List<Market> markets = marketService.getNearby(lat, lng, radiusKm, destinationId);
        return ResponseEntity.ok(markets);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateMarket(@PathVariable long id, @RequestBody MarketUpdateRequest request) {
        marketService.updateMarket(id, request.name, request.location, 
                request.operatingDays, request.operatingHours, request.description);
        return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, MARKET));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMarket(@PathVariable long id) {
        String result = marketService.deleteMarket(id);
        return ResponseEntity.ok(result);
    }

    // Inner class for update requests
    public static class MarketUpdateRequest {
        public String name;
        public String location;
        public String operatingDays;
        public String operatingHours;
        public String description;
    }
}
