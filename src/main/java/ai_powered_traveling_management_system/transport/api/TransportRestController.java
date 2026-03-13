package ai_powered_traveling_management_system.transport.api;

import ai_powered_traveling_management_system.transport.entities.Transport;
import ai_powered_traveling_management_system.transport.service.TransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transports")
public class TransportRestController {
    @Autowired
    private TransportService transportService;

    @PostMapping("/add")
    public String postTransport(@RequestBody Transport transport) {
        return transportService.addTransport(transport);
    }

    @GetMapping("/al")
    public List<Transport> getAllTransports () {
        return transportService.getAllTransport();
    }
}
