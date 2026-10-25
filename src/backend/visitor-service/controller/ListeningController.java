package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import BUS.ListeningBUS;
import DTO.ListeningEventDTO;

@RestController
public class ListeningController {

    private final ListeningBUS bus;

    public ListeningController(ListeningBUS bus) {
        this.bus = bus;
    }

    @PostMapping("/api/visitor/listening-events")
    public ResponseEntity<Void> record(
            @RequestHeader(
                    name = "X-Visitor-Token",
                    required = false
            ) String token,
            @RequestBody ListeningEventDTO request
    ) {
        bus.record(token, request);

        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }
}