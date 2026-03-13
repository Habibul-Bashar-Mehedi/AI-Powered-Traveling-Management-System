package ai_powered_traveling_management_system.chat_history.api;

import ai_powered_traveling_management_system.chat_history.entities.ChatHistory;
import ai_powered_traveling_management_system.chat_history.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/history")
public class ChatHistoryRestController {
    @Autowired
    private ChatHistoryService chatHistoryService;

    @PostMapping("/add")
    public String postChatHistory(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.addChatHistory(chatHistory);
    }

    @GetMapping("/all")
    public List<ChatHistory> getAllChatHistories () {
        return chatHistoryService.getAllChatHistory();
    }
}
