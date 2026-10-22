package com.sgu.audioguide.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sgu.audioguide.BUS.NarrationBUS;
import com.sgu.audioguide.DTO.AdminNarrationWriteDTO;
import com.sgu.audioguide.DTO.NarrationDTO;
import com.sgu.audioguide.config.AdminAccess;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/narrations")
public class AdminNarrationController {

    private final NarrationBUS bus;
    private final AdminAccess admin;

    public AdminNarrationController(
            NarrationBUS bus,
            AdminAccess admin
    ) {
        this.bus = bus;
        this.admin = admin;
    }

    @GetMapping
    public List<NarrationDTO> all(HttpServletRequest request) {
        admin.requireAdmin(request);
        return bus.all();
    }

    @GetMapping("/{id}")
    public NarrationDTO detail(
            @PathVariable("id") long id,
            HttpServletRequest request
    ) {
        admin.requireAdmin(request);
        return bus.getAdminDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NarrationDTO create(
            @RequestBody AdminNarrationWriteDTO body,
            HttpServletRequest request
    ) {
        admin.requireAdmin(request);
        return bus.createFromAdmin(body);
    }

    @PutMapping("/{id}")
    public NarrationDTO update(
            @PathVariable("id") long id,
            @RequestBody AdminNarrationWriteDTO body,
            HttpServletRequest request
    ) {
        admin.requireAdmin(request);
        return bus.updateFromAdmin(id, body);
    }
}
