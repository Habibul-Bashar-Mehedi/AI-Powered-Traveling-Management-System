package aptms.api;

import aptms.entities.Booking;
import aptms.entities.ChatHistory;
import aptms.entities.Destination;
import aptms.entities.Hotel;
import aptms.entities.Room;
import aptms.entities.Market;
import aptms.entities.Route;
import aptms.services.AdminService;
import aptms.entities.TouristSpot;
import aptms.entities.TraditionalFood;
import aptms.entities.TraditionalItem;
import aptms.entities.Transport;
import aptms.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {
    @Autowired
    private AdminService adminService;
    //======================Put Mapping=======================

    //user
    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(@PathVariable long id, @RequestBody User user) {

         boolean update = adminService.updateUser(id,user.getUsername(), user.getEmail(), user.getPassword(), user.getRole(),user.getCountryId());
         if(update) {
             return ResponseEntity.ok("user updated successfully");
         }else {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body("user not found with id : "+id);
         }
    }

    //transport
    @PutMapping("/transports/{id}")
    public ResponseEntity<String> updateTransport(@PathVariable long id ,@RequestBody Transport transport){
        boolean update = adminService.updateTransport(id,transport.getModel(),transport.getOperatorName(),transport.getEstimatedCost(),
                transport.getEstimatedDuration(),transport.getFrequency());

        if (update) {
            return ResponseEntity.ok("transport updated successfully done");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("transport id not found with id: "+id);
        }

    }

    //traditional item
    @PutMapping("/traditionals/item/{id}")
    public ResponseEntity<String> updateTraditionalItem(@PathVariable long id,@RequestBody TraditionalItem traditionalItem) {
        boolean update = adminService.updateTraditionalItem(id,traditionalItem.getCategoryName(),traditionalItem.getDescription(),traditionalItem.getPriceRange());

        if(update) {
            return ResponseEntity.ok("traditional item updated");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("traditional item not found with id: "+id);
        }

    }

    //traditional food
    @PutMapping("/traditional/foods/{id}")
    public ResponseEntity<String> updateTraditionalFood(@PathVariable long id, @RequestBody TraditionalFood traditionalFood) {

        boolean update = adminService.updateTraditionalFood(id,traditionalFood.getDishName(),
                traditionalFood.getDescription(), traditionalFood.getCulturalContext(),
                traditionalFood.getPriceRange(), traditionalFood.getRecommendedLocation());

        if(update) {
            return ResponseEntity.ok("traditional food updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("traditional food not found with id: "+id);
        }
    }

    //tourist spot
    @PutMapping("/tourist/spots/{id}")
    public ResponseEntity<String> updateTouristSpot(@PathVariable long id, @RequestBody TouristSpot touristSpot) {
        boolean update = adminService.updateTouristSpot(id,touristSpot.getName(),
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
    //route
    @PutMapping("/routes/{id}")
    public ResponseEntity<String> updateRoute(@PathVariable long id, @RequestBody Route route) {
        boolean update = adminService.updateRoute(id,route.getDistanceKM(),
            route.getEstimatedDuration(), route.getRouteDescription());

        if(update) {
            return ResponseEntity.ok("route updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("route not found with id: "+id);
        }
    }

    //markets
    @PutMapping("/markets/{id}")
    public ResponseEntity<String> updateMarket(@PathVariable long id, @RequestBody Market market) {
        boolean update = adminService.updateMarket(id,market.getName(), market.getLocation(),
                market.getOperatingDays(), market.getOperatingHours(), market.getDescription());

        if(update) {
            return ResponseEntity.ok("market updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("market not found with id: "+id);
        }
    }

    //hotel
    @PutMapping("/hotels/{id}")
    public ResponseEntity<String> updateHotel(@PathVariable long id, @RequestBody Hotel hotel) {
        boolean update = adminService.updateHotel(id,hotel.getHotelName(),
            hotel.getAddress(), hotel.getStatus(), hotel.getDescriptions());

        if(update) {
            return ResponseEntity.ok("hotel updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("hotel not found with id: "+id);
        }
    }

    //room
    @PutMapping("/rooms/{id}")
    public ResponseEntity<String> updateRoom(@PathVariable long id, @RequestBody Room room) {
        boolean update = adminService.updateRoom(id,room.getRoomTypeName(),
        room.getAmenities(),
        room.getPricePerNight(),
        room.getAvailableQuantities(),
        room.getStatus());

        if(update) {
            return ResponseEntity.ok("room updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("room not found with id: "+id);
        }
    }

    //destination
    @PutMapping("/destinations/{id}")
    public ResponseEntity<String> updateDestination(@PathVariable long id, @RequestBody Destination destination) {
        boolean update = adminService.updateDestination(id,destination.getName(),
            destination.getRegion(), destination.getDescription());

        if(update) {
            return ResponseEntity.ok("destination updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("destination not found with id: "+id);
        }
    }

    //chat history
    @PutMapping("/chat/histories/{id}")
    public ResponseEntity<String> updateChatHistory(@PathVariable long id, @RequestBody ChatHistory chatHistory) {
        boolean update = adminService.updateChatHistory(id,chatHistory.getAiResponse(), chatHistory.getSessionId());

        if(update) {
            return ResponseEntity.ok("chat history updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("chat history not found with id: "+id);
        }
    }

    //booking
    @PutMapping("/bookinges/{id}")
    public ResponseEntity<String> updateBooking(@PathVariable long id, @RequestBody Booking booking) {
        boolean update = adminService.updateBooking(id,booking.getCheckInDate(),
            booking.getCheckOutDate(), booking.getGuestCount(), booking.getTotalPrice(),
                booking.getStatus(), booking.getSpecialRequest());

        if(update) {
            return ResponseEntity.ok("booking updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("booking not found with id: "+id);
        }
    }

}
