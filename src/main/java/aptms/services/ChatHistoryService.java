package aptms.services;

import aptms.entities.ChatHistory;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatHistoryService {
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    public ChatHistory addChatHistory(ChatHistory chatHistory) {
        if(chatHistory.getUserInput() == null || chatHistory.getSessionId() == null) {
            throw new InvalidException("user input and session id required");
        }
        boolean exists =
                chatHistoryRepository
                        .existsByUserInputAndAiResponseAndSessionId(
                                chatHistory.getUserInput(),
                                chatHistory.getAiResponse(),
                                chatHistory.getSessionId()
                        );
        if (exists) {
            throw new DuplicateValueFoundExceptions("this chat history already added");
        }
        return chatHistoryRepository.save(chatHistory);
    }

    public List<ChatHistory> getAllChatHistory() {
        return chatHistoryRepository.findAll();
    }

    //chat history
    public String deleteChatHistory(long id) {
        if(!chatHistoryRepository.existsById(id)) throw new IdNotFoundException("chat history id not found");

        chatHistoryRepository.deleteById(id);
        return "chat history is deleted";
    }

    public boolean updateChatHistory(long id,
                                     String aiResponse,String sessionId) {

        return chatHistoryRepository.findById(id).map(chatHistory -> {
            chatHistory.setAiResponse(aiResponse);
            chatHistory.setSessionId(sessionId);

            chatHistoryRepository.save(chatHistory);
            return true;
        }).orElseThrow(()->
                new IdNotFoundException("chat history id not found")
        );
    }
}
