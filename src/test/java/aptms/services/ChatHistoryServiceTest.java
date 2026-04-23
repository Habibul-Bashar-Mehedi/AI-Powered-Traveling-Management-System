package aptms.services;

import aptms.entities.ChatHistory;
import aptms.entities.Transport;
import aptms.entities.User;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.ChatHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatHistoryServiceTest {
    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @InjectMocks
    private ChatHistoryService chatHistoryService;

    private ChatHistory testChat;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);

        testChat = new ChatHistory();
        testChat.setUser(testUser);
        testChat.setUserInput("hello Ai");
        testChat.setAiResponse("hello buddy");
        testChat.setSessionId("20str");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        testChat.setCreatedAt(calendar.getTime());
    }

    @Test
    void addChatHistoryTest() {
        when(chatHistoryRepository
                .existsByUserInputAndAiResponseAndSessionId(
                        testChat.getUserInput(),
                        testChat.getAiResponse(),
                        testChat.getSessionId())).thenReturn(false
        );

        when(chatHistoryRepository.save(any(ChatHistory.class))).thenReturn(testChat);

        ChatHistory result =chatHistoryService.addChatHistory(testChat);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(10L);
        verify(chatHistoryRepository,times(1)).save(testChat);
    }

    @Test
    void alreadyExists() {
        when(chatHistoryRepository
                .existsByUserInputAndAiResponseAndSessionId(
                        testChat.getUserInput(),
                        testChat.getAiResponse(),
                        testChat.getSessionId())).thenReturn(true
        );

        assertThrows(DuplicateValueFoundExceptions.class,()->{
            chatHistoryService.addChatHistory(testChat);
        });

        verify(chatHistoryRepository,never()).save(any(ChatHistory.class));

    }

    @Test
    void missingChatHistoryTest() {
        testChat.setUserInput(null);

        assertThrows(InvalidException.class,()->{
            chatHistoryService.addChatHistory(testChat);
        });
    }

    @Test
    void deleteChatTest() {
        Long id = 1L;

        when(chatHistoryRepository.existsById(id)).thenReturn(true);
        String response = chatHistoryService.deleteChatHistory(id);

        assertEquals("chat history is deleted",response);
        verify(chatHistoryRepository,times(1)).deleteById(id);
    }

    @Test
    void updateTest() {
        long id = 1L;
        String newUserInput = "hey";
        String newAiResponse = "hey buddy";
        when(chatHistoryRepository.findById(id)).thenReturn(java.util.Optional.of(testChat));

        boolean isUpdated = chatHistoryService.updateChatHistory(id,newUserInput,newAiResponse);

        assertThat(isUpdated).isTrue();
        verify(chatHistoryRepository,times(1)).save(any(ChatHistory.class));
    }

    @Test
    void getAllChatHistoryTest() {
        when(chatHistoryRepository.findAll()).thenReturn(List.of(testChat));
        List<ChatHistory> chatHistories = chatHistoryService.getAllChatHistory();
        assertEquals(1, chatHistories.size());
    }

}
