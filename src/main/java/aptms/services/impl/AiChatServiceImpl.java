package aptms.services.impl;

import aptms.dto.AiChatRequest;
import aptms.dto.AiChatResponse;
import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import aptms.repositories.VendorServiceRepository;
import aptms.services.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s";
    private static final int MAX_HISTORY_TURNS = 8;
    private static final int MAX_PACKAGES_IN_CONTEXT = 20;
    private static final String NOT_CONFIGURED_REPLY =
            "AI assistant is not configured. Please contact support.";
    private static final String ERROR_REPLY =
            "Sorry, the AI assistant is temporarily unavailable. Please try again shortly.";

    private final RestTemplate restTemplate;
    private final VendorServiceRepository vendorServiceRepository;

    @Value("${app.ai.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini-model:gemini-1.5-flash}")
    private String geminiModel;

    public AiChatServiceImpl(RestTemplate restTemplate, VendorServiceRepository vendorServiceRepository) {
        this.restTemplate = restTemplate;
        this.vendorServiceRepository = vendorServiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AiChatResponse chat(AiChatRequest request) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new AiChatResponse(NOT_CONFIGURED_REPLY);
        }

        String username = (request.getUsername() == null || request.getUsername().isBlank())
                ? null : request.getUsername();
        String systemPrompt = buildSystemPrompt(username);

        List<Map<String, Object>> contents = new ArrayList<>();
        List<AiChatRequest.AiChatTurn> history = request.getHistory();
        if (history != null) {
            int from = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (AiChatRequest.AiChatTurn turn : history.subList(from, history.size())) {
                contents.add(toContentPart(turn.getSender(), turn.getText()));
            }
        }
        contents.add(toContentPart("user", request.getMessage()));

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", contents
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        String url = String.format(GEMINI_URL_TEMPLATE, geminiModel, geminiApiKey);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, httpEntity, Map.class);
            String reply = extractReply(response);
            return new AiChatResponse(reply != null ? reply : "Sorry, please try again.");
        } catch (RestClientException ex) {
            log.warn("Gemini API call failed: {}", ex.getMessage());
            return new AiChatResponse(ERROR_REPLY);
        }
    }

    private String buildSystemPrompt(String username) {
        StringBuilder prompt = new StringBuilder(
                "You are an expert AI travel assistant for a premium travel booking platform. " +
                "Help users plan trips, recommend destinations, suggest itineraries, estimate budgets, " +
                "and answer travel-related questions. Keep responses concise, friendly, and professional. " +
                "Use emojis sparingly for a warm tone."
        );
        if (username != null) {
            prompt.append(" The user's name is ").append(username).append(".");
        }

        prompt.append(" Your most important job is helping the user pick the best tour package for their budget. ")
              .append(buildTourPackageContext());

        return prompt.toString();
    }

    /**
     * Grounds tour recommendations in the platform's real, currently-active listings so the
     * assistant never invents packages, prices, or vendors that don't actually exist.
     */
    private String buildTourPackageContext() {
        List<VendorService> packages = vendorServiceRepository
                .findByStatusAndServiceType(
                        ServiceStatus.ACTIVE,
                        ServiceType.TOUR_PACKAGE,
                        PageRequest.of(0, MAX_PACKAGES_IN_CONTEXT, Sort.by("updatedAt").descending()))
                .getContent();

        if (packages.isEmpty()) {
            return "There are currently no tour packages published on the platform. " +
                   "If asked for a recommendation, say so honestly and suggest the user check back soon " +
                   "or explore the \"Available Services\" section on their dashboard for other service types.";
        }

        StringBuilder context = new StringBuilder(
                "Here is the current list of ACTIVE tour packages available on the platform " +
                "(name | vendor | price | pricing unit | description). " +
                "Only recommend packages from this list — never invent a package, vendor, or price. " +
                "When the user gives a budget, recommend the package(s) that best fit it, explain briefly why, " +
                "and mention the exact package name and vendor so they can find it. " +
                "If nothing fits their budget, say so honestly and suggest the closest option or a cheaper alternative. " +
                "Always tell the user they can book their chosen package from the \"Available Services\" " +
                "section on their dashboard.\n"
        );

        for (VendorService svc : packages) {
            String vendorName = svc.getVendor() != null ? svc.getVendor().getBusinessName() : "Unknown vendor";
            String description = svc.getDescription() == null ? "" :
                    svc.getDescription().substring(0, Math.min(140, svc.getDescription().length()));
            context.append("- ")
                   .append(svc.getServiceName()).append(" | ")
                   .append(vendorName).append(" | ")
                   .append(svc.getCurrencyCode()).append(' ').append(svc.getBasePrice()).append(" | ")
                   .append(svc.getPricingUnit()).append(" | ")
                   .append(description).append('\n');
        }

        return context.toString();
    }

    private Map<String, Object> toContentPart(String sender, String text) {
        String role = "user".equalsIgnoreCase(sender) ? "user" : "model";
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        try {
            List<Object> candidates = (List<Object>) response.get("candidates");
            Map<String, Object> firstCandidate = (Map<String, Object>) candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Object> parts = (List<Object>) content.get("parts");
            Map<String, Object> firstPart = (Map<String, Object>) parts.get(0);
            return (String) firstPart.get("text");
        } catch (RuntimeException ex) {
            log.warn("Unexpected Gemini response shape: {}", response);
            return null;
        }
    }
}
