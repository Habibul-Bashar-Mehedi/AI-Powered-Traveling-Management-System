package aptms.services;

import aptms.dto.AiChatRequest;
import aptms.dto.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
}
