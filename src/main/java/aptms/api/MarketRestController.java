package aptms.api;

import aptms.entities.Market;
import aptms.services.MarketService;
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
