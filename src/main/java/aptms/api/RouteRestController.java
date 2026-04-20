package aptms.api;

import aptms.entities.Route;
import aptms.services.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteRestController {
    private final RouteService routeService;

    public RouteRestController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/add")
    public Route postRoute(@RequestBody Route route) {
        return routeService.addRoute(route);
    }

    @GetMapping()
    public List<Route> getAllRoutes () {
        return routeService.getAllRoute();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateRoute(@PathVariable long id, @RequestBody Route route) {
        boolean update = routeService.updateRoute(id,route.getDistanceKM(),
                route.getEstimatedDuration(), route.getRouteDescription());

        if(update) {
            return ResponseEntity.ok("route updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("route not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoute(@PathVariable long id) {
        String result = routeService.deleteRoute(id);
        if(result.equals("route is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
