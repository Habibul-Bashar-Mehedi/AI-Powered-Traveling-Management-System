package aptms.services;

import aptms.entities.ChatHistory;
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
            throw new RuntimeException("user input and session id required");
        }
        boolean exists =
                chatHistoryRepository
                        .existsByUserInputAndAiResponseAndSessionId(
                                chatHistory.getUserInput(),
                                chatHistory.getAiResponse(),
                                chatHistory.getSessionId()
                        );
        if (exists) {
            throw new RuntimeException("this chat history already added");
        }
        return chatHistoryRepository.save(chatHistory);
    }

    public List<ChatHistory> getAllChatHistory() {
        return chatHistoryRepository.findAll();
    }

    //chat history
    public String deleteChatHistory(long id) {
        if(!chatHistoryRepository.existsById(id)) return "chat history not found";

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
        }).orElse(false);
    }
}
