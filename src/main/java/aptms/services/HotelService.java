package aptms.services;

import aptms.entities.Hotel;
import aptms.repositories.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<Hotel> getAllHotel() {
        return hotelRepository.findAll();
    }
}
