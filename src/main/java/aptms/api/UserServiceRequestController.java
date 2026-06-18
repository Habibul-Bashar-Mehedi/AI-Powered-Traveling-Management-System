package aptms.api;

import aptms.dto.vendor.UserServiceRequestDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.services.UserServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/service-requests")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "User Service Requests", description = "Create user requests that appear in vendor booking inbox")
public class UserServiceRequestController {

    private final UserServiceRequestService userServiceRequestService;

    @PostMapping
    @Operation(summary = "Create a service request from dashboard actions")
    public ResponseEntity<VendorBookingDTO> create(@Valid @RequestBody UserServiceRequestDTO request) {
        VendorBookingDTO booking = userServiceRequestService.createRequest(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping
    @Operation(summary = "Get all service requests submitted by the logged-in user")
    public ResponseEntity<List<VendorBookingDTO>> getMyBookings() {
        return ResponseEntity.ok(userServiceRequestService.getMyBookings(getCurrentUserId()));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(Objects.requireNonNull(auth).getName());
    }
}




