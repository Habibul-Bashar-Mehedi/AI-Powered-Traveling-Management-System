package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Hotel;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
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
        validateHotelData(hotel.getHotelName(), hotel.getAddress());

        boolean exists = hotelRepository.existsHotelByHotelNameAndAddress(
                hotel.getHotelName(),
                hotel.getAddress()
        );

        if (exists) {
            throw new DuplicateValueFoundExceptions("Hotel already exists with this name and address.");
        }

        return hotelRepository.save(hotel);
    }


    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Hotel> getAllHotel() {
        return hotelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Hotel getHotelById(long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException("Hotel not found with id: " + id));
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteHotel(long id) {
        if (!hotelRepository.existsById(id)) {
            throw new IdNotFoundException("Cannot delete. Hotel id not found: " + id);
        }

        hotelRepository.deleteById(id);
        return "Hotel has been successfully deleted.";
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateHotel(long id, String hotelName, String address, String status, String descriptions) {

        validateHotelData(hotelName, address);

        boolean alreadyExists = hotelRepository.existsHotelByHotelNameAndAddress(hotelName, address);

        if (alreadyExists) {
            throw new DuplicateValueFoundExceptions("Another hotel with this name and address already exists!");
        }

        return hotelRepository.findById(id).map(hotel -> {
            hotel.setHotelName(hotelName);
            hotel.setAddress(address);
            hotel.setStatus(status);
            hotel.setDescriptions(descriptions);
            hotelRepository.save(hotel);
            return true;
        }).orElseThrow(() -> new IdNotFoundException("Hotel id not found: " + id));
    }


    private void validateHotelData(String name, String address) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidException("Hotel name is required and cannot be empty.");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new InvalidException("Hotel address is required.");
        }
    }
}