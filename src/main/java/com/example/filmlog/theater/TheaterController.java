package com.example.filmlog.theater;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/theaters", produces = MediaType.APPLICATION_JSON_VALUE)
public class TheaterController {

    private final TheaterService service;

    public TheaterController(TheaterService service) {
        this.service = service;
    }

    /** 위도·경도 주변의 영화관을 가까운 순으로 돌려줍니다. 좌표는 저장하지 않습니다. */
    @GetMapping
    public List<Theater> nearby(@RequestParam double lat, @RequestParam double lng,
                                @RequestParam(defaultValue = "5000") int radius) {
        if (!(lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 좌표입니다.");
        }
        return service.find(lat, lng, Math.max(500, Math.min(radius, 20000)));
    }
}
