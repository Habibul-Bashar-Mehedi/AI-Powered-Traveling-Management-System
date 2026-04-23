package aptms.services;

import aptms.entities.Market;
import aptms.entities.TraditionalItem;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TraditionalItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TraditionalItemServiceTest {

    @Mock
    private TraditionalItemRepository traditionalItemRepository;

    @InjectMocks
    private TraditionalItemService traditionalItemService;

    private Market testMarket;
    private TraditionalItem testItem;

    @BeforeEach
    void setUp() {
        testMarket = new Market();
        testMarket.setId(30L);

        testItem = new TraditionalItem();
        testItem.setId(1L);
        testItem.setMarket(testMarket);
        testItem.setCategoryName("Handicrafts");
        testItem.setDescription("Traditional handmade clay pottery");
        testItem.setPriceRange("500-2000 BDT");
    }

    @Test
    void addTraditionalItemTest() {
        when(traditionalItemRepository.existsTraditionalItemByMarketIdAndCategoryName(anyLong(), anyString())).thenReturn(false);
        when(traditionalItemRepository.save(any(TraditionalItem.class))).thenReturn(testItem);

        TraditionalItem result = traditionalItemService.addTraditionalItem(testItem);

        assertNotNull(result);
        assertEquals("Handicrafts", result.getCategoryName());
        verify(traditionalItemRepository, times(1)).save(testItem);
    }

    @Test
    void missingTraditionalItemTest() {
        testItem.setMarket(null);
        assertThrows(InvalidException.class, () -> traditionalItemService.addTraditionalItem(testItem));
    }

    @Test
    void alreadyExistsTest() {
        when(traditionalItemRepository.existsTraditionalItemByMarketIdAndCategoryName(anyLong(), anyString())).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> traditionalItemService.addTraditionalItem(testItem));
    }

    @Test
    void getAllTraditionalItemTest() {
        when(traditionalItemRepository.findAll()).thenReturn(List.of(testItem));
        List<TraditionalItem> items = traditionalItemService.getAllTraditionalItem();
        assertEquals(1, items.size());
    }

    @Test
    void deleteTraditionalItemTest() {
        when(traditionalItemRepository.existsById(1L)).thenReturn(true);

        String response = traditionalItemService.deleteTraditionalItem(1L);

        assertEquals("traditional item is deleted", response);
        verify(traditionalItemRepository, times(1)).deleteById(1L);
    }

    @Test
    void updateTraditionalItemTest() {
        when(traditionalItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        boolean result = traditionalItemService.updateTraditionalItem(
                1L, "Nakshi Kantha", "Embroidered quilt", "3000-8000 BDT"
        );

        assertTrue(result);
        assertEquals("Nakshi Kantha", testItem.getCategoryName());
        assertEquals("3000-8000 BDT", testItem.getPriceRange());
        verify(traditionalItemRepository, times(1)).save(testItem);
    }
}