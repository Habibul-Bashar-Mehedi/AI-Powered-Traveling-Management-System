package aptms.repositories;

import aptms.entities.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory , Long> {

    boolean existsByUserInputAndAiResponseAndSessionId(String userInput, String aiResponse, String sessionId);
}
