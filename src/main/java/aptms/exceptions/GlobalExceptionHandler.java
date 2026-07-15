package aptms.exceptions;

import aptms.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 * 
 * Handles validation errors, authentication failures, and other exceptions
 * with consistent error response format.
 * 
 * Requirements: 4.3.2, FR-REG-003, FR-LGN-002, FR-MID-003, FR-MID-004
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors from @Valid annotations.
     * Returns HTTP 400 with field-specific error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        String message = "Validation failed: " + fieldErrors.toString();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message(message)
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.warn("Validation error: {}", message);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle duplicate value exceptions (e.g., email already exists).
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateValueFoundExceptions.class)
    public ResponseEntity<ErrorResponse> handleDuplicateValue(
            DuplicateValueFoundExceptions ex,
            WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("DUPLICATE_VALUE")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.warn("Duplicate value error: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle invalid credentials and authentication failures.
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidException(
            InvalidException ex,
            WebRequest request) {
        
        // Check if this is an account lockout error
        HttpStatus status = ex.getMessage().contains("locked") 
            ? HttpStatus.LOCKED 
            : HttpStatus.UNAUTHORIZED;
        
        String errorCode = ex.getMessage().contains("locked") 
            ? "ACCOUNT_LOCKED" 
            : "INVALID_CREDENTIALS";
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(errorCode)
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.warn("Authentication error: {}", ex.getMessage());
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle OTP verification/resend failures.
     * Returns HTTP 400 (429 for resend cooldown) with a specific error code.
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponse> handleOtpException(
            OtpException ex,
            WebRequest request) {

        HttpStatus status = ex.getErrorCode() == OtpException.ErrorCode.OTP_RESEND_COOLDOWN
            ? HttpStatus.TOO_MANY_REQUESTS
            : HttpStatus.BAD_REQUEST;

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getErrorCode().name())
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();

        logger.warn("OTP error: {} - {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle login attempts on accounts still pending email verification.
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(
            EmailNotVerifiedException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("EMAIL_NOT_VERIFIED")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();

        logger.warn("Login rejected, email not verified: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle entity not found errors.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(IdNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            IdNotFoundException ex,
            WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.warn("Not found error: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle expired JWT tokens.
     * Returns HTTP 401 with TOKEN_EXPIRED error code.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex,
            WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("TOKEN_EXPIRED")
            .message("Access token has expired")
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.debug("Expired token: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle invalid JWT tokens (signature mismatch, malformed, etc.).
     * Returns HTTP 401 with TOKEN_INVALID error code.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex,
            WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("TOKEN_INVALID")
            .message("Invalid or malformed token")
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.debug("Invalid token: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("BAD_REQUEST")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.warn("Bad request: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle database unique-constraint and FK violations.
     *
     * Spring wraps JDBC SQLIntegrityConstraintViolationException inside
     * DataIntegrityViolationException. Without this handler the generic
     * Exception.class handler would swallow it and return HTTP 500.
     * Returns HTTP 409 Conflict with a safe, user-facing message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {

        // Extract the most specific root-cause message for logging, but never
        // expose raw SQL or internal table names in the response body.
        String rootMsg = ex.getMostSpecificCause().getMessage();
        String userMessage;
        if (rootMsg != null && rootMsg.toLowerCase().contains("duplicate")) {
            userMessage = "A record with the same value already exists. "
                    + "Please check registration number, tax ID, or email.";
        } else {
            userMessage = "The request could not be completed due to a data conflict.";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("DATA_CONFLICT")
                .message(userMessage)
                .timestamp(Instant.now())
                .path(getRequestPath(request))
                .build();

        logger.warn("Data integrity violation: {}", rootMsg);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle illegal state exceptions (business rule violations).
     * E.g. "Booking is not in PENDING state", "Insufficient wallet balance".
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("CONFLICT")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();

        logger.warn("Business rule conflict: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle uploads that exceed the servlet-level multipart size ceiling
     * (spring.servlet.multipart.max-file-size / max-request-size).
     * Returns HTTP 413 Payload Too Large with a clear message, instead of
     * falling through to the generic 500 handler.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("PAYLOAD_TOO_LARGE")
            .message("The uploaded file is too large.")
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();

        logger.warn("Upload rejected: exceeds max multipart size: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Pass ResponseStatusException through with its intended status code.
     * Used by SecurityUtils and controllers that throw explicit 401/403/404.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getStatusCode().toString())
            .message(ex.getReason() != null ? ex.getReason() : ex.getMessage())
            .timestamp(java.time.Instant.now())
            .path(getRequestPath(request))
            .build();

        logger.debug("ResponseStatusException: {} {}", ex.getStatusCode(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle all other exceptions.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .timestamp(Instant.now())
            .path(getRequestPath(request))
            .build();
        
        logger.error("Unexpected error: ", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Extract request path from WebRequest.
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}
