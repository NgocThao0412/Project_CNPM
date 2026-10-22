package com.sgu.audioguide.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sgu.audioguide.BUS.VisitorBUS;
import com.sgu.audioguide.DTO.VisitorHeartbeatDTO;
import com.sgu.audioguide.DTO.VisitorSessionDTO;
import com.sgu.audioguide.DTO.VisitorStatsDTO;
import com.sgu.audioguide.config.AdminAccess;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class VisitorController {

    private final VisitorBUS bus;
    private final AdminAccess access;

    public VisitorController(VisitorBUS bus, AdminAccess access) {
        this.bus = bus;
        this.access = access;
    }

    @PostMapping("/api/visitor/sessions")
    public ResponseEntity<VisitorSessionDTO> create() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Cache-Control", "no-store")
                .body(bus.create());
    }

    @PostMapping("/api/visitor/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestHeader(
                    name = "X-Visitor-Token",
                    required = false
            ) String token,
            @RequestBody VisitorHeartbeatDTO request
    ) {
        bus.heartbeat(token, request);

        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }

    @PostMapping("/api/visitor/end")
    public ResponseEntity<Void> end(
            @RequestHeader(
                    name = "X-Visitor-Token",
                    required = false
            ) String token
    ) {
        bus.end(token);

        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }

    @GetMapping("/api/admin/visitor-stats")
    public ResponseEntity<VisitorStatsDTO> stats(
            HttpServletRequest request
    ) {
        access.requireAdmin(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(bus.stats());
    }
}