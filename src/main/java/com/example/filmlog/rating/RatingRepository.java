package com.example.filmlog.rating;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByVisitorId(String visitorId);

    Optional<Rating> findByVisitorIdAndMovieId(String visitorId, Long movieId);

    long countByMovieId(Long movieId);

    @Query("select avg(r.score) from Rating r where r.movieId = :movieId")
    Double averageByMovieId(@Param("movieId") Long movieId);
}
