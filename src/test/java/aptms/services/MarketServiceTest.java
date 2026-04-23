package aptms.services;

import aptms.entities.Destination;
import aptms.entities.Market;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.MarketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarketServiceTest {
    @Mock
    private MarketRepository marketRepository;

    @InjectMocks
    private MarketService marketService;
    private Destination testDestination;
    private Market testMarket;

    @BeforeEach
    void setUp() {
        testDestination = new Destination();
        testDestination.setId(10L);
        testMarket = new Market();
        testMarket.setId(1L);
        testMarket.setDestination(testDestination);
        testMarket.setName("taltola market");
        testMarket.setDescription("electronic products");
        testMarket.setAlive(true);
        testMarket.setLocation("dhaka khilgao");
        testMarket.setDeleted(false);
        testMarket.setOperatingDays("test days");
        testMarket.setOperatingHours("12h");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        testMarket.setCreatedAt(calendar.getTime());
    }

    @Test
    void addMarketTest() {
        when(marketRepository
                .existsMarketsByNameAndDestinationId(
                        testMarket.getName(),
                        testDestination.getId())
        ).thenReturn(false);

        when(marketRepository.save(any(Market.class))).thenReturn(testMarket);

        Market result = marketService.addMarket(testMarket);

        assertThat(result).isNotNull();
        assertThat(result.getDestination().getId()).isEqualTo(10L);
        assertEquals("electronic products",testMarket.getDescription());
        assertEquals("dhaka khilgao",testMarket.getLocation());
        assertEquals("taltola market",testMarket.getName());
        assertEquals("test days",testMarket.getOperatingDays());
        assertEquals("12h",testMarket.getOperatingHours());
        verify(marketRepository,times(1)).save(testMarket);

    }

    @Test
    void alreadyExist() {
        when(marketRepository
                .existsMarketsByNameAndDestinationId(
                        testMarket.getName(),
                        testDestination.getId())
        ).thenReturn(true);

        assertThrows(DuplicateValueFoundExceptions.class,()->{
            marketService.addMarket(testMarket);
        });

        verify(marketRepository,never()).save(testMarket);
    }

    @Test
    void missingMarketTest() {
        testMarket.setName(null);
        assertThrows(InvalidException.class,()->{
           marketService.addMarket(testMarket);
        });
    }

    @Test
    void deleteTest() {
        long id = 1L;

        when(marketRepository.existsById(id)).thenReturn(true);

        String response = marketService.deleteMarket(id);

        assertEquals("market is deleted",response);

        verify(marketRepository, never()).save(any(Market.class));
    }
    @Test
    void updateMarket() {
        long id = 1L;
        String newName ="10 tala market";
        String newLocation = "banasree";
        String newOperatingDays = "gas";
        String newOperatingHours = "10h";
        String newDescription = "varieties items";

        when(marketRepository.findById(id)).thenReturn(Optional.of(testMarket));

        boolean isUpdate = marketService.updateMarket(id,newName,newLocation,newOperatingDays,newOperatingHours,newDescription);

        assertThat(isUpdate).isTrue();
        verify(marketRepository,times(1)).save(any(Market.class));
    }

    @Test
    void getAllMarketTest() {
        when(marketRepository.findAll()).thenReturn(List.of(testMarket));
        List<Market> markets = marketService.getAllMarket();
        assertEquals(1,markets.size());
    }

}
