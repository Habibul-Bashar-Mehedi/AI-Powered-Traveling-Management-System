package ai_powered_traveling_management_system.hotel.services;

import ai_powered_traveling_management_system.hotel.entities.Hotel;
import ai_powered_traveling_management_system.hotel.repositories.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HotelService {
    @Autowired
    private HotelRepository hotelRepository;

    public String addHotel(Hotel hotel) {
        boolean exists = hotelRepository.existsHotelByHotelNameAndAddress(hotel.getHotelName(),hotel.getAddress());
        if(exists) {
            throw  new RuntimeException("Hotel already exists");
        }
        hotelRepository.save(hotel);
        return "Hotel successfully added";
    }
}
