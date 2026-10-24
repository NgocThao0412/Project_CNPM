package controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import BUS.POIBUS;
import DTO.AdminPOIDTO;
import DTO.CampusAreaDTO;
import client.AuthServiceClient;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class POICatalogController {

    private final POIBUS poiBUS;
    private final AuthServiceClient authServiceClient;

    public POICatalogController(
            POIBUS poiBUS,
            AuthServiceClient authServiceClient
    ) {
        this.poiBUS = poiBUS;
        this.authServiceClient = authServiceClient;
    }

    @GetMapping("/api/admin/pois")
    public List<AdminPOIDTO> adminPOIs(
            HttpServletRequest request
    ) {
        authServiceClient.requireAdmin(request);
        return poiBUS.getAdminPOIs();
    }

    @GetMapping("/api/campus-areas")
    public List<CampusAreaDTO> areas() {
        return poiBUS.getVisibleAreas();
    }
}