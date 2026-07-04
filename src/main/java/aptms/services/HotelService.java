package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Hotel;
import aptms.enums.HotelStatus;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.HotelRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class HotelService {

    private static final int MAX_LIST_SIZE = 500;

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
            throw new DuplicateValueFoundExceptions(
                String.format(ENTITY_ALREADY_EXISTS_MESSAGE, HOTEL)
            );
        }
        
        // Set default status if not provided
        if(hotel.getStatus() == null) {
            hotel.setStatus(HotelStatus.ACTIVE);
        }

        return hotelRepository.save(hotel);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Hotel> getAllHotel() {
        return hotelRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional(readOnly = true)
    public Hotel getHotelById(long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, HOTEL, id)
                ));
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteHotel(long id) {
        if (!hotelRepository.existsById(id)) {
            throw new IdNotFoundException(
                String.format(ENTITY_NOT_FOUND_MESSAGE, HOTEL, id)
            );
        }

        hotelRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, HOTEL);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateHotel(long id, String hotelName, String address, 
                              HotelStatus status, String descriptions) {

        validateHotelData(hotelName, address);

        boolean alreadyExists = hotelRepository.existsHotelByHotelNameAndAddress(hotelName, address);

        if (alreadyExists) {
            throw new DuplicateValueFoundExceptions(
                String.format(DUPLICATE_ENTRY_MESSAGE, HOTEL)
            );
        }

        return hotelRepository.findById(id).map(hotel -> {
            hotel.setHotelName(hotelName);
            hotel.setAddress(address);
            hotel.setStatus(status);
            hotel.setDescriptions(descriptions);
            hotelRepository.save(hotel);
            return true;
        }).orElseThrow(() -> new IdNotFoundException(
            String.format(ENTITY_NOT_FOUND_MESSAGE, HOTEL, id)
        ));
    }

    private void validateHotelData(String name, String address) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidException(
                String.format(REQUIRED_FIELD_MESSAGE, "Hotel name")
            );
        }
        if (address == null || address.trim().isEmpty()) {
            throw new InvalidException(
                String.format(REQUIRED_FIELD_MESSAGE, FIELD_ADDRESS)
            );
        }
    }
}