package aptms.api;

import aptms.entities.Transport;
import aptms.services.TransportService;
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

    @GetMapping("/all")
    public List<Transport> getAllTransports () {
        return transportService.getAllTransport();
    }
}
