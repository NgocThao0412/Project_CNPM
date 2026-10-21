package com.sgu.audioguide.controller;

import com.sgu.audioguide.BUS.POIBUS;
import com.sgu.audioguide.DTO.POIDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pois")
public class POIController {

    private final POIBUS poiBUS;

    public POIController(POIBUS poiBUS) {
        this.poiBUS = poiBUS;
    }

    @GetMapping
    public List<POIDTO> getVisiblePOIs() {
        return poiBUS.getVisiblePOIs();
    }
}