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

    public Hotel addHotel(Hotel hotel) {
        boolean exists = hotelRepository.existsHotelByHotelNameAndAddress(hotel.getHotelName(),hotel.getAddress());
        if(exists) {
            throw  new RuntimeException("Hotel already exists");
        }
        return hotelRepository.save(hotel);
    }

    public List<Hotel> getAllHotel() {
        return hotelRepository.findAll();
    }

    //hotel
    public String deleteHotel(long id) {
        if(!hotelRepository.existsById(id)) return "hotel not found";

        hotelRepository.deleteById(id);
        return "hotel is deleted";
    }

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
}
