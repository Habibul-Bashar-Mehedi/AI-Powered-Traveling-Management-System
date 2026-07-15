package aptms.services.impl;

import aptms.dto.AiChatRequest;
import aptms.dto.AiChatResponse;
import aptms.dto.BookingHistoryDTO;
import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import aptms.repositories.VendorServiceRepository;
import aptms.security.SecurityUtils;
import aptms.services.AiChatService;
import aptms.services.BookingHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s";
    private static final int MAX_HISTORY_TURNS = 8;
    private static final int MAX_PACKAGES_IN_CONTEXT = 20;
    private static final int MAX_GEMINI_RETRIES = 3;
    private static final long RETRY_DELAY_BASE_MS = 2000L;
    private static final int MAX_BOOKINGS_IN_CONTEXT = 5;
    private static final String NOT_CONFIGURED_REPLY =
            "AI assistant is not configured. Please contact support.";

    private final RestTemplate restTemplate;
    private final VendorServiceRepository vendorServiceRepository;
    private final BookingHistoryService bookingHistoryService;

    @Value("${app.ai.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini-model:gemini-2.0-flash}")
    private String geminiModel;

    public AiChatServiceImpl(RestTemplate restTemplate,
                             VendorServiceRepository vendorServiceRepository,
                             BookingHistoryService bookingHistoryService) {
        this.restTemplate = restTemplate;
        this.vendorServiceRepository = vendorServiceRepository;
        this.bookingHistoryService = bookingHistoryService;
    }

    @PostConstruct
    void logConfig() {
        boolean keyPresent = geminiApiKey != null && !geminiApiKey.isBlank();
        log.info("Gemini API key: {}", keyPresent ? "Present" : "Missing");
        log.info("Gemini model: {}", geminiModel);
        if (keyPresent && geminiApiKey.length() < 20) {
            log.warn("GEMINI_API_KEY is suspiciously short ({} chars) — may be invalid", geminiApiKey.length());
        }
        if (!keyPresent) {
            log.error("GEMINI_API_KEY is not set. The AI chat will always return a fallback message. " +
                      "Set GEMINI_API_KEY=AIzaSy... in .env or as an environment variable. " +
                      "Get a free key at https://aistudio.google.com/apikey");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AiChatResponse chat(AiChatRequest request) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key is missing or blank — chat unavailable");
            return new AiChatResponse(NOT_CONFIGURED_REPLY);
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        String username = (request.getUsername() == null || request.getUsername().isBlank())
                ? null : request.getUsername();
        String systemPrompt = buildSystemPrompt(username, userId);

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(toContentPart("user", systemPrompt));
        List<AiChatRequest.AiChatTurn> history = request.getHistory();
        if (history != null) {
            int from = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (AiChatRequest.AiChatTurn turn : history.subList(from, history.size())) {
                contents.add(toContentPart(turn.getSender(), turn.getText()));
            }
        }
        contents.add(toContentPart("user", request.getMessage()));

        Map<String, Object> body = Map.of(
                "contents", contents
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        String url = String.format(GEMINI_URL_TEMPLATE, geminiModel, geminiApiKey);

        try {
            String reply = callGeminiWithRetry(url, httpEntity);
            return new AiChatResponse(reply != null ? reply : "Sorry, please try again.");
        } catch (HttpStatusCodeException ex) {
            String googleMsg = extractGoogleErrorMessage(ex.getResponseBodyAsString());
            log.warn("Gemini API HTTP error: {} — {}",
                    ex.getStatusCode(), googleMsg != null ? googleMsg : ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Gemini rate limit exhausted after {} retries", MAX_GEMINI_RETRIES);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "The AI service is currently experiencing high demand. Please try again later.");
            }
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                String detail = googleMsg != null ? ": " + googleMsg : "";
                log.error("Gemini API authentication failed — check GEMINI_API_KEY. Google says: {}", googleMsg != null ? googleMsg : "no details");
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI service authentication failed." + detail + " Verify GEMINI_API_KEY in .env is correct. Get a free key at https://aistudio.google.com/apikey");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service is temporarily unavailable. Please try again shortly.");
        } catch (ResourceAccessException ex) {
            log.warn("Gemini API network/timeout error: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service is temporarily unreachable. Please try again.");
        } catch (RuntimeException ex) {
            log.error("Unexpected error calling Gemini API", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred while processing your request.");
        }
    }

    private String callGeminiWithRetry(String url, HttpEntity<Map<String, Object>> httpEntity) {
        for (int attempt = 1; attempt <= MAX_GEMINI_RETRIES; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(url, httpEntity, Map.class);
                return extractReply(response);
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < MAX_GEMINI_RETRIES) {
                    long delay = RETRY_DELAY_BASE_MS + (long) (Math.random() * 1000);
                    log.warn("Gemini 429 (attempt {}/{}), retrying in {}ms", attempt, MAX_GEMINI_RETRIES, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Request was interrupted. Please try again.");
                    }
                    continue;
                }
                throw ex;
            }
        }
        throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Gemini rate limit retries exhausted");
    }

    private String buildSystemPrompt(String username, UUID userId) {
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

        prompt.append("\n\n").append(buildUserBookingContext(userId));

        return prompt.toString();
    }

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

    private String buildUserBookingContext(UUID userId) {
        List<BookingHistoryDTO> bookings = bookingHistoryService.getAllHistory(userId);

        List<BookingHistoryDTO> recent = bookings.size() > MAX_BOOKINGS_IN_CONTEXT
                ? bookings.subList(0, MAX_BOOKINGS_IN_CONTEXT)
                : bookings;

        if (recent.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder(
                "The user has the following recent/past bookings on the platform. " +
                "Use these to provide personalized recommendations, mention their upcoming trips, " +
                "or suggest related activities. " +
                "Do not invent bookings the user doesn't have.\n"
        );

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        for (BookingHistoryDTO b : recent) {
            context.append("- ");
            if (b.getTravelDate() != null) {
                String travelDate = b.getTravelDate() instanceof LocalDate
                        ? ((LocalDate) b.getTravelDate()).format(dateFormatter)
                        : b.getTravelDate().toString();
                context.append("Travel: ").append(travelDate).append(" | ");
            }
            context.append(b.getTitle());
            if (b.getAmount() != null && b.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                context.append(" | Amount: $").append(b.getAmount());
            }
            context.append(" | Status: ").append(b.getStatus());
            context.append('\n');
        }

        return context.toString();
    }

    private String extractGoogleErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> body = mapper.readValue(responseBody, Map.class);
            Map<?, ?> error = (Map<?, ?>) body.get("error");
            if (error != null) {
                String msg = (String) error.get("message");
                if (msg != null) return msg;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Map<String, Object> toContentPart(String sender, String text) {
        String role = "user".equalsIgnoreCase(sender) ? "user" : "model";
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        if (response == null) {
            log.warn("Gemini returned null response");
            return null;
        }
        try {
            List<Object> candidates = (List<Object>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                Map<String, Object> feedback = (Map<String, Object>) response.get("promptFeedback");
                String blockReason = feedback != null ? (String) feedback.get("blockReason") : null;
                if (blockReason != null) {
                    log.warn("Gemini response blocked by safety filter: {}", blockReason);
                    return "I'm sorry, I cannot provide an answer to that request due to content safety guidelines. Could you please rephrase your question?";
                }
                log.warn("Gemini returned empty candidates list");
                return "Sorry, I couldn't generate a response. Please try rephrasing your question.";
            }
            Map<String, Object> firstCandidate = (Map<String, Object>) candidates.get(0);
            String finishReason = (String) firstCandidate.get("finishReason");
            if (finishReason != null && !"STOP".equals(finishReason)) {
                log.warn("Gemini finishReason: {} — response may be incomplete or blocked", finishReason);
            }
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            if (content == null) {
                log.warn("Gemini candidate has no content");
                return null;
            }
            List<Object> parts = (List<Object>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                log.warn("Gemini content has no parts");
                return null;
            }
            Map<String, Object> firstPart = (Map<String, Object>) parts.get(0);
            return (String) firstPart.get("text");
        } catch (RuntimeException ex) {
            log.warn("Unexpected Gemini response shape: {}", response, ex);
            return null;
        }
    }
}
