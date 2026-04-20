package aptms.api;

import aptms.entities.ChatHistory;
import aptms.services.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/history")
public class ChatHistoryRestController {
    private final ChatHistoryService chatHistoryService;

    public ChatHistoryRestController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @PostMapping("/add")
    public ChatHistory postChatHistory(@RequestBody ChatHistory chatHistory) {
        return chatHistoryService.addChatHistory(chatHistory);
    }

    @GetMapping()
    public List<ChatHistory> getAllChatHistories () {
        return chatHistoryService.getAllChatHistory();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateChatHistory(@PathVariable long id, @RequestBody ChatHistory chatHistory) {
        boolean update = chatHistoryService.updateChatHistory(id,chatHistory.getAiResponse(), chatHistory.getSessionId());

        if(update) {
            return ResponseEntity.ok("chat history updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("chat history not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteChatHistory(@PathVariable long id) {
        String result = chatHistoryService.deleteChatHistory(id);
        if(result.equals("chat history is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
