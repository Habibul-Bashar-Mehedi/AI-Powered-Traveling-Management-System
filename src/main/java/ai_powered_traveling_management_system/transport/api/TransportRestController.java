package ai_powered_traveling_management_system.transport.api;

import ai_powered_traveling_management_system.transport.entities.Transport;
import ai_powered_traveling_management_system.transport.service.TransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transports")
public class TransportRestController {
    @Autowired
    private TransportService transportService;

    @PostMapping("/add")
    public String postTransport(@RequestBody Transport transport) {
        return transportService.addTransport(transport);
    }
}
