package ai_powered_traveling_management_system.chat_history.repositories;

import ai_powered_traveling_management_system.chat_history.entities.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory , Long> {

    boolean existsByUserInputAndAiResponseAndSessionId(String userInput, String aiResponse, String sessionId);
}
