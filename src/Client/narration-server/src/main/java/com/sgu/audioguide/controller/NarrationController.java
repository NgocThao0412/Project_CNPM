package com.sgu.audioguide.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgu.audioguide.BUS.NarrationBUS;
import com.sgu.audioguide.DTO.NarrationDTO;
import com.sgu.audioguide.DTO.NarrationWriteDTO;
import com.sgu.audioguide.config.AdminAccess;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/narrations")
public class NarrationController {

    private final NarrationBUS bus;
    private final AdminAccess admin;

    public NarrationController(NarrationBUS bus, AdminAccess admin) {
        this.bus = bus;
        this.admin = admin;
    }

    @GetMapping
    public List<NarrationDTO> visible(
            @RequestParam(name = "poiId", required = false) Long poiId,
            @RequestParam(name = "languageCode", required = false) String languageCode
    ) {
        return bus.visible(poiId, languageCode);
    }

    @GetMapping("/admin")
    public List<NarrationDTO> all(HttpServletRequest r) {
        admin.requireAdmin(r);
        return bus.all();
    }

    @PostMapping
    public NarrationDTO create(@RequestBody NarrationWriteDTO n, HttpServletRequest r) {
        admin.requireAdmin(r);
        return bus.create(n);
    }

    @PutMapping("/{id}")
    public NarrationDTO update(@PathVariable long id, @RequestBody NarrationWriteDTO n, HttpServletRequest r) {
        admin.requireAdmin(r);
        return bus.update(id, n);
    }

    @PatchMapping("/{id}/visibility")
    public Map<String, Object> visibility(@PathVariable long id, @RequestBody Map<String, Boolean> m, HttpServletRequest r) {
        admin.requireAdmin(r);
        Boolean v = m.get("isVisible");
        if (v == null) {
            throw new IllegalArgumentException("Thiếu isVisible");
        }
        bus.visibility(id, v);
        return Map.of("id", id, "isVisible", v);
    }
}
