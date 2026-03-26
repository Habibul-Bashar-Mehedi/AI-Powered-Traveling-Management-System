package aptms.api;

import aptms.entities.ChatHistory;
import aptms.services.ChatHistoryService;
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
