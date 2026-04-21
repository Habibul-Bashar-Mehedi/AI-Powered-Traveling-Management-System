package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Hotel;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Hotel addHotel(Hotel hotel) {
        boolean exists = hotelRepository.existsHotelByHotelNameAndAddress(hotel.getHotelName(),hotel.getAddress());
        if(exists) {
            throw  new DuplicateValueFoundExceptions("Hotel already exists");
        }
        return hotelRepository.save(hotel);
    }


    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Hotel> getAllHotel() {
        return hotelRepository.findAll();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteHotel(long id) {
        if(!hotelRepository.existsById(id)) throw new IdNotFoundException("hotel id not found");

        hotelRepository.deleteById(id);
        return "hotel is deleted";
    }


    @Transactional
    @SecureAction(role = "ADMIN")
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
        }).orElseThrow(()->
                new IdNotFoundException("hotel id not found"));

    }
}
