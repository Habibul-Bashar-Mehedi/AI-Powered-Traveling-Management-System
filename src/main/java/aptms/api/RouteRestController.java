package aptms.api;

import aptms.entities.Route;
import aptms.services.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteRestController {
    @Autowired
    private RouteService routeService;

    @PostMapping("/add")
    public String postRoute(@RequestBody Route route) {
        return routeService.addRoute(route);
    }

    @GetMapping("/all")
    public List<Route> getAllRoutes () {
        return routeService.getAllRoute();
    }
}
