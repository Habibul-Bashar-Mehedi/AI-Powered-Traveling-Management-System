package aptms.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AiChatRequest {

    private String username;
    @NotBlank(message = "Message must not be empty")
    private String message;
    private List<AiChatTurn> history;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiChatTurn> getHistory() {
        return history;
    }

    public void setHistory(List<AiChatTurn> history) {
        this.history = history;
    }

    public static class AiChatTurn {
        private String sender;
        private String text;

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
