package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.ChatHistory;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.ChatHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;

    public ChatHistoryService(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
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

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<ChatHistory> getAllChatHistory() {
        return chatHistoryRepository.findAll();
    }

    @SecureAction(role = "ADMIN")
    public String deleteChatHistory(long id) {
        if(!chatHistoryRepository.existsById(id)) throw new IdNotFoundException("chat history id not found");

        chatHistoryRepository.deleteById(id);
        return "chat history is deleted";
    }

    @Transactional
    @SecureAction(role = "ADMIN")
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
