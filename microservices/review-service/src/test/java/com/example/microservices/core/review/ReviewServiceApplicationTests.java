package com.example.microservices.core.review;

import static org.junit.jupiter.api.Assertions.*;
import static reactor.core.publisher.Mono.just;

import com.example.api.core.review.Review;
import com.example.microservices.core.review.persistence.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewServiceApplicationTests extends MySqlTestBase {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ReviewRepository repository;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
    }

    @Test
    void contextLoads() {
    }


	@Test
	void getReviewsByProductId() {
		int productId = 1;
        assertEquals(0, repository.findByProductId(productId).size());

        postAndVerifyReview(productId, 1, HttpStatus.OK);
        postAndVerifyReview(productId, 2, HttpStatus.OK);
        postAndVerifyReview(productId, 3, HttpStatus.OK);

        assertEquals(3, repository.findByProductId(productId).size());

        getAndVerifyReviewsByProductId(1, HttpStatus.OK)
				.jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].reviewId").isEqualTo(3);
	}

    @Test
    void duplicateError() {
        int productId = 1;
        int reviewId = 1;
        assertEquals(0, repository.count());
        postAndVerifyReview(productId, reviewId, HttpStatus.OK)
                .jsonPath("$.productId").isEqualTo(productId)
                .jsonPath("$.reviewId").isEqualTo(reviewId);
        assertEquals(1, repository.count());
        postAndVerifyReview(productId, reviewId, HttpStatus.UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/reviews")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Review Id: 1");
        assertEquals(1, repository.count());
    }

    @Test
    void deleteReviews() {
        int productId = 1;
        int reviewId = 1;

        postAndVerifyReview(productId, reviewId, HttpStatus.OK);
        assertEquals(1, repository.findByProductId(productId).size());

        deleteAndVerifyReviewsByProductId(productId, HttpStatus.OK);
        assertEquals(0, repository.findByProductId(productId).size());

        deleteAndVerifyReviewsByProductId(productId, HttpStatus.OK);
    }

	@Test
	void getReviewsMissingParameter() {
        getAndVerifyReviewsByProductId("", HttpStatus.BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/reviews")
				.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	void getReviewsInvalidParameter() {
        getAndVerifyReviewsByProductId("?productId=no-integer", HttpStatus.BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/reviews")
				.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	void getReviewsInvalidParameterNegativeValue() {
		int productIdInvalid = -1;
        getAndVerifyReviewsByProductId(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/reviews")
				.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(
            int productId, HttpStatus expectedStatus
    ) {
        return getAndVerifyReviewsByProductId(
                "?productId=" + productId,
                expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(
            String productIdQuery, HttpStatus expectedStatus
    ) {
        return client.get()
                .uri("/reviews" + productIdQuery)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec postAndVerifyReview(int productId,
                                                              int reviewId,
                                                              HttpStatus expectedStatus) {
        Review review = new Review(productId, reviewId, "Author " + reviewId,
                "Subject " + reviewId, "Content " + reviewId, "SA");
        return client.post()
                .uri("/reviews")
                .body(just(review), Review.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(
            int productId, HttpStatus expectedStatus
    ) {
        return client.delete()
                .uri("/reviews?productId=" + productId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody();
    }

}
