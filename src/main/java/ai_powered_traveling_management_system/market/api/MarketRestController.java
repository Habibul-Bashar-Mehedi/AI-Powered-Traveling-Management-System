package ai_powered_traveling_management_system.market.api;

import ai_powered_traveling_management_system.market.entities.Market;
import ai_powered_traveling_management_system.market.services.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketRestController {
    @Autowired
    private MarketService marketService;

    @PostMapping("/add")
    public String postMarket(@RequestBody Market market) {
        return marketService.addMarket(market);
    }
}
