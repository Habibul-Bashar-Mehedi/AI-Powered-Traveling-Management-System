package aptms.exceptions;

import aptms.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

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
