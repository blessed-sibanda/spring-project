package com.example.api.core.review;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {
  @PostMapping(
          value = "/reviews",
          consumes = "application/json",
          produces = "application/json"
  )
  Mono<Review> createReview(@RequestBody Review body);

  @GetMapping(value = "/reviews", produces = "application/json")
  Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

  @DeleteMapping(value = "/reviews")
  Mono<Void> deleteReviews(@RequestParam(value = "productId", required = true)
                     int productId);
}
