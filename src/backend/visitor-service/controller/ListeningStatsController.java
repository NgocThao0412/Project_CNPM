package controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import BUS.ListeningStatsBUS;
import DTO.ListeningStatsDTO;
import config.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ListeningStatsController {

    private final ListeningStatsBUS bus;
    private final AdminAccess access;

    public ListeningStatsController(
            ListeningStatsBUS bus,
            AdminAccess access
    ) {
        this.bus = bus;
        this.access = access;
    }

    @GetMapping("/api/admin/listening-stats")
    public ResponseEntity<ListeningStatsDTO> stats(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(name = "poiId", required = false)
            Long poiId,

            @RequestParam(
                    name = "languageCode",
                    required = false
            )
            String languageCode,

            HttpServletRequest request
    ) {
        access.requireAdmin(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(bus.stats(
                        from,
                        to,
                        poiId,
                        languageCode
                ));
    }
}