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

    public String addChatHistory(ChatHistory chatHistory) {
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
        chatHistoryRepository.save(chatHistory);
        return "chat history successfully added";
    }

    public List<ChatHistory> getAllChatHistory() {
        return chatHistoryRepository.findAll();
    }
}
