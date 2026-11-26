package com.example.filmlog.geocode;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/geocode", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeocodeController {

    private final GeocodeService service;

    public GeocodeController(GeocodeService service) {
        this.service = service;
    }

  
    @GetMapping
    public List<Place> geocode(@RequestParam String query) {
        String q = query.trim();
        if (q.isEmpty() || q.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소를 1~100자로 입력하세요.");
        }
        return service.search(q);
    }
}
