package ai_powered_traveling_management_system.route.api;

import ai_powered_traveling_management_system.route.entities.Route;
import ai_powered_traveling_management_system.route.services.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
public class RouteRestController {
    @Autowired
    private RouteService routeService;

    @PostMapping("/add")
    public String postRoute(@RequestBody Route route) {
        return routeService.addRoute(route);
    }
}
