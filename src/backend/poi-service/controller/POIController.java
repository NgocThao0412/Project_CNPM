package controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import BUS.POIBUS;
import DTO.POIDTO;

@RestController
@RequestMapping("/api/pois")
public class POIController {

    private final POIBUS poiBUS;

    public POIController(POIBUS poiBUS) {
        this.poiBUS = poiBUS;
    }

    @GetMapping
    public List<POIDTO> getVisiblePOIs(
            @RequestParam(name = "keyword", defaultValue = "") String keyword
    ) {
        try {
            return poiBUS.searchVisiblePOIs(keyword);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    @GetMapping("/{id}")
    public POIDTO getVisiblePOIById(@PathVariable("id") Long id) {
        try {
            return poiBUS.getVisiblePOIById(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        } catch (NoSuchElementException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    exception.getMessage()
            );
        }
    }
}
