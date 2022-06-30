package com.example.api.core.review;

import java.util.List;

import org.springframework.web.bind.annotation.*;

public interface ReviewService {
  @PostMapping(
          value = "/reviews",
          consumes = "application/json",
          produces = "application/json"
  )
  Review createReview(@RequestBody Review body);

  @GetMapping(value = "/reviews", produces = "application/json")
  List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

  @DeleteMapping(value = "/reviews")
  void deleteReviews(@RequestParam(value = "productId", required = true)
                     int productId);
}
