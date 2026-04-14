package aptms.api;

import aptms.entities.TouristSpot;
import aptms.services.TouristSpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tourist/spot")
public class TouristSpotRestController {
    @Autowired
    private TouristSpotService touristSpotService;

    @PostMapping("/add")
    public TouristSpot postTouristSpot(@RequestBody TouristSpot touristSpot) {
        return touristSpotService.addTouristSpot(touristSpot);
    }

    @GetMapping()
    public List<TouristSpot> getAllTouristSpots() {
        return touristSpotService.getAllTouristSpot();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateTouristSpot(@PathVariable long id, @RequestBody TouristSpot touristSpot) {
        boolean update = touristSpotService.updateTouristSpot(id,touristSpot.getName(),
                touristSpot.getDescription(), touristSpot.getVisitingHours(),
                touristSpot.getAdultEntryFees(), touristSpot.getChildEntryFees(),
                touristSpot.getLocationDescription());

        if(update) {
            return ResponseEntity.ok("tourist spot updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("tourist spot not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTouristSpot(@PathVariable long id) {
        String result = touristSpotService.deleteTouristSpot(id);
        if(result.equals("tourist spot is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
