package ai_powered_traveling_management_system.chat_history.api;

import ai_powered_traveling_management_system.chat_history.entities.ChatHistory;
import ai_powered_traveling_management_system.chat_history.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/history")
public class ChatHistoryRestController {
    @Autowired
    private ChatHistoryService chatHistoryService;

    @PostMapping("/add")
    public String postChatHistory(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.addChatHistory(chatHistory);
    }
}
