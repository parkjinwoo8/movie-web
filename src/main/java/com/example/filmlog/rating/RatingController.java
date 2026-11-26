package com.example.filmlog.rating;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    public record ScoreRequest(int score) {}

    public record Summary(double average, long count) {}

    private final RatingRepository repo;
    private final VisitorResolver visitor;

    public RatingController(RatingRepository repo, VisitorResolver visitor) {
        this.repo = repo;
        this.visitor = visitor;
    }

    /*내가 남긴 별점 */
    @GetMapping("/mine")
    public Map<Long, Integer> mine(HttpServletRequest req, HttpServletResponse res) {
        String uid = visitor.resolve(req, res);
        return repo.findByVisitorId(uid).stream()
                .collect(Collectors.toMap(Rating::getMovieId, Rating::getScore));
    }

    /* 별점 등록 또는 수정 */
    @PutMapping("/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rate(@PathVariable long movieId, @RequestBody ScoreRequest body,
                     HttpServletRequest req, HttpServletResponse res) {
        if (body.score() < 1 || body.score() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "별점은 1~5 사이여야 합니다.");
        }
        String uid = visitor.resolve(req, res);
        Rating r = repo.findByVisitorIdAndMovieId(uid, movieId).orElseGet(() -> {
            Rating n = new Rating();
            n.setVisitorId(uid);
            n.setMovieId(movieId);
            return n;
        });
        r.setScore(body.score());
        r.setUpdatedAt(Instant.now());
        repo.save(r);
    }

    /* 별점 취소 */
    @DeleteMapping("/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable long movieId, HttpServletRequest req, HttpServletResponse res) {
        String uid = visitor.resolve(req, res);
        repo.findByVisitorIdAndMovieId(uid, movieId).ifPresent(repo::delete);
    }

    /* 전체 방문자 평균 별점 */
    @GetMapping("/{movieId}/summary")
    public Summary summary(@PathVariable long movieId) {
        Double avg = repo.averageByMovieId(movieId);
        return new Summary(avg == null ? 0 : avg, repo.countByMovieId(movieId));
    }
}
