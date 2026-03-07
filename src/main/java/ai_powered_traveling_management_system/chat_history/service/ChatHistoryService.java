package ai_powered_traveling_management_system.chat_history.service;

import ai_powered_traveling_management_system.chat_history.entities.ChatHistory;
import ai_powered_traveling_management_system.chat_history.repositories.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
