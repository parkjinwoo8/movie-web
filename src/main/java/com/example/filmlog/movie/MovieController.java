package com.example.filmlog.movie;

import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.filmlog.tmdb.TmdbClient;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class MovieController {

    private static final Set<String> MODES = Set.of("popular", "top_rated", "now_playing", "upcoming");

    private final TmdbClient tmdb;

    public MovieController(TmdbClient tmdb) {
        this.tmdb = tmdb;
    }

    @GetMapping("/movies")
    public String list(@RequestParam(defaultValue = "popular") String mode,
                       @RequestParam(defaultValue = "1") int page) {
        if (!MODES.contains(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 목록입니다.");
        }
        return tmdb.get("/movie/" + mode, Map.of("page", String.valueOf(clamp(page)), "region", "KR"));
    }

    @GetMapping("/movies/search")
    public String search(@RequestParam String query, @RequestParam(defaultValue = "1") int page) {
        if (query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력하세요.");
        }
        return tmdb.get("/search/movie", Map.of("query", query.trim(), "page", String.valueOf(clamp(page))));
    }

    @GetMapping("/movies/{id}")
    public String detail(@PathVariable long id) {
        return tmdb.get("/movie/" + id, Map.of("append_to_response", "credits"));
    }

    @GetMapping("/genres")
    public String genres() {
        return tmdb.get("/genre/movie/list", Map.of());
    }

    private static int clamp(int page) {
        return Math.max(1, Math.min(page, 500));   // TMDB는 최대 500페이지
    }
}
