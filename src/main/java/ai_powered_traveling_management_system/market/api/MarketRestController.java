package ai_powered_traveling_management_system.market.api;

import ai_powered_traveling_management_system.market.entities.Market;
import ai_powered_traveling_management_system.market.services.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketRestController {
    @Autowired
    private MarketService marketService;

    @PostMapping("/add")
    public String postMarket(@RequestBody Market market) {
        return marketService.addMarket(market);
    }

    @GetMapping("/all")
    public List<Market> getAllMarket() {
        return marketService.getAllMarket();
    }
}
