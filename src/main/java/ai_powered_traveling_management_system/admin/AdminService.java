package ai_powered_traveling_management_system.admin;

import ai_powered_traveling_management_system.booking.repositories.BookingRepository;
import ai_powered_traveling_management_system.chat_history.repositories.ChatHistoryRepository;
import ai_powered_traveling_management_system.destination.repositories.DestinationRepository;
import ai_powered_traveling_management_system.hotel.repositories.HotelRepository;
import ai_powered_traveling_management_system.hotel.repositories.RoomRepository;
import ai_powered_traveling_management_system.market.repositories.MarketRepository;
import ai_powered_traveling_management_system.route.repositoies.RouteRepository;
import ai_powered_traveling_management_system.tourist_spot.repositories.TouristSpotRepository;
import ai_powered_traveling_management_system.traditional_food.repositories.TraditionalFoodRepository;
import ai_powered_traveling_management_system.traditional_item.repositories.TraditionalItemRepository;
import ai_powered_traveling_management_system.transport.repository.TransportRepository;
import ai_powered_traveling_management_system.user.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AdminService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private MarketRepository marketRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private TouristSpotRepository touristSpotRepository;
    @Autowired
    private TraditionalFoodRepository traditionalFoodRepository;
    @Autowired
    private TraditionalItemRepository traditionalItemRepository;
    @Autowired
    private TransportRepository transportRepository;
    @Autowired
    private UserRepository userRepository;

    //update section
    
    // user
    public boolean updateUser(
            long id,String username,
            String email,String password,
            String role,String countryId) {

        return userRepository.findById(id).map(user -> {
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setRole(role);
            user.setCountryId(countryId);

            userRepository.save(user);
            return true;
        }).orElse(false);
    }
    
    //transport
    public boolean updateTransport(long id ,String model,
                                   String operatorName ,double estimatedCost,
                                   String estimatedDuration ,String frequency) {
        return transportRepository.findById(id).map(transport -> {
            transport.setModel(model);
            transport.setOperatorName(operatorName);
            transport.setEstimatedCost(estimatedCost);
            transport.setEstimatedDuration(estimatedDuration);
            transport.setFrequency(frequency);
            
            transportRepository.save(transport);
            
            return true;
        }).orElse(false);
    }
    //traditional item
    public boolean updateTraditionalItem(long id,String categoryName,
                                         String description,String priceRange) {
        return traditionalItemRepository.findById(id).map(traditionalItem -> {
            traditionalItem.setCategoryName(categoryName);
            traditionalItem.setDescription(description);
            traditionalItem.setPriceRange(priceRange);
            
            traditionalItemRepository.save(traditionalItem);
            
            return true;
        }).orElse(false);
    }
    //traditional food
    public boolean updateTraditionalFood(long id, String dishName,
                                         String description,String culturalContext,
                                         String priceRange,String recommendedLocation) {
        return traditionalFoodRepository.findById(id).map(traditionalFood -> {
            traditionalFood.setDishName(dishName);
            traditionalFood.setDescription(description);
            traditionalFood.setCulturalContext(culturalContext);
            traditionalFood.setPriceRange(priceRange);
            traditionalFood.setRecommendedLocation(recommendedLocation);
            
            traditionalFoodRepository.save(traditionalFood);
            
            return true;
        }).orElse(false);
        
    }
    
    // tourist spot
    public boolean updateTouristSpot(long id,String name,
                                     String description,String visitingHours,
                                     double adultEntryFees,double childEntryFees,
                                     String locationDescription) {
        
        return touristSpotRepository.findById(id).map(touristSpot -> {
            touristSpot.setName(name);
            touristSpot.setDescription(description);
            touristSpot.setVisitingHours(visitingHours);
            touristSpot.setAdultEntryFees(adultEntryFees);
            touristSpot.setChildEntryFees(childEntryFees);
            touristSpot.setLocationDescription(locationDescription);

            touristSpotRepository.save(touristSpot);
            
            return true;
        }).orElse(false);
        
    }
    //route
    public boolean updateRoute(long id,double distanceKM,
                               String estimatedDuration,String routeDescription) {

        return routeRepository.findById(id).map(route -> {

            route.setDistanceKM(distanceKM);
            route.setEstimatedDuration(estimatedDuration);
            route.setRouteDescription(routeDescription);

            routeRepository.save(route);
            return true;
        }).orElse(false);

    }

    //market
    public boolean updateMarket(long id ,String name,String location,
                                String operatingDays,String operatingHours,
                                String description) {

        return marketRepository.findById(id).map(market -> {
            market.setName(name);
            market.setLocation(location);
            market.setOperatingDays(operatingDays);
            market.setOperatingHours(operatingHours);
            market.setDescription(description);

            marketRepository.save(market);
            return true;
        }).orElse(false);

    }

    //hotel
    public boolean updateHotel(long id,String hotelName,
                               String address,String status,
                               String descriptions) {
        return hotelRepository.findById(id).map(hotel -> {
            hotel.setHotelName(hotelName);
            hotel.setAddress(address);
            hotel.setStatus(status);
            hotel.setDescriptions(descriptions);

            hotelRepository.save(hotel);
            return true;
        }).orElse(false);

    }

    //room
    public boolean updateRoom(long id,String roomTypeName,
                              String amenities,double pricePerNight,
                              int availableQuantities,String status) {

        return roomRepository.findById(id).map(room -> {
            room.setRoomTypeName(roomTypeName);
            room.setAmenities(amenities);
            room.setPricePerNight(pricePerNight);
            room.setAvailableQuantities(availableQuantities);
            room.setStatus(status);

            roomRepository.save(room);
            return true;
        }).orElse(false);

    }

    //destination
    public boolean updateDestination(long id,String name, String region,
                                     String description) {

        return destinationRepository.findById(id).map(destination -> {
            destination.setName(name);
            destination.setRegion(region);
            destination.setDescription(description);

            destinationRepository.save(destination);
            return true;
        }).orElse(false);
    }

    //chat history

    public boolean updateChatHistory(long id,
                                     String aiResponse,String sessionId) {

        return chatHistoryRepository.findById(id).map(chatHistory -> {
            chatHistory.setAiResponse(aiResponse);
            chatHistory.setSessionId(sessionId);

            chatHistoryRepository.save(chatHistory);
            return true;
        }).orElse(false);
    }

    //booking
    public boolean updateBooking(long id, Date checkInDate,
                                 Date checkOutDate,int guestCount,
                                 Double totalPrice,String status,
                                 String specialRequest) {

        return bookingRepository.findById(id).map(booking -> {
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);
            booking.setGuestCount(guestCount);
            booking.setTotalPrice(totalPrice);
            booking.setStatus(status);
            booking.setSpecialRequest(specialRequest);

            bookingRepository.save(booking);
            return true;
        }).orElse(false);

    }

    //delete section

    //user
    public String deleteUser(long id) {
        if(!userRepository.existsById(id)) {
            return "user not found";
        }
        userRepository.deleteById(id);
        return "user is deleted";
    }

    //transport
    public String deleteTransport(long id) {
        if(!transportRepository.existsById(id)) {
            return "transport not found";
        }
        transportRepository.deleteById(id);
        return "transport is deleted";
    }

    //traditional food
    public String deleteTraditionalFood(long id) {
        if(!traditionalFoodRepository.existsById(id)) return "traditional food not found";

        traditionalFoodRepository.deleteById(id);
        return "traditional food is deleted";
    }

    //traditional item
    public String deleteTraditionalItem(long id) {
        if(!traditionalItemRepository.existsById(id)) return "traditional item not found";

        traditionalItemRepository.deleteById(id);
        return "traditional item is deleted";
    }

    //tourist spot
    public String deleteTouristSpot(long id) {
        if(!touristSpotRepository.existsById(id)) return "tourist spot not found";

        touristSpotRepository.deleteById(id);
        return "tourist spot is deleted";
    }

    //market
    public String deletedMarket(long id) {
        if(!marketRepository.existsById(id)) return "market  not found";

        marketRepository.deleteById(id);
        return "market is deleted";
    }

    //hotel
    public String deletedHotel(long id) {
        if(!hotelRepository.existsById(id)) return "hotel  not found";

        hotelRepository.deleteById(id);
        return "hotel is deleted";
    }

    //room
    public String deletedRoom(long id) {
        if(!roomRepository.existsById(id)) return "room  not found";

        roomRepository.deleteById(id);
        return "room is deleted";
    }

    //destination
    public String deletedDestination(long id) {
        if(!destinationRepository.existsById(id)) return "destination  not found";

        destinationRepository.deleteById(id);
        return "destination is deleted";
    }

    //chat history
    public String deletedChatHistory(long id) {
        if(!chatHistoryRepository.existsById(id)) return "chat history  not found";

        chatHistoryRepository.deleteById(id);
        return "chat history is deleted";
    }

    //booking
    public String deletedBooking(long id) {
        if(!bookingRepository.existsById(id)) return "booking not found";

        bookingRepository.deleteById(id);
        return "booking is deleted";
    }
}