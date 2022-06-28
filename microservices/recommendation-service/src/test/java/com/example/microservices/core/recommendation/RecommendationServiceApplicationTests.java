package com.example.microservices.core.recommendation;

import static reactor.core.publisher.Mono.just;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.api.core.recommendation.Recommendation;
import com.example.microservices.core.recommendation.persistence.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationServiceApplicationTests extends MongoDbTestBase {
	@Autowired
	private WebTestClient client;

	@Autowired
	private RecommendationRepository repository;

	@BeforeEach
	void setupDb() {
		repository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getRecommendationsByProductId() {
		int productId = 1;
		postAndVerifyRecommendation(productId, 1, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 2, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 3, HttpStatus.OK);

		assertEquals(3, repository.findByProductId(productId).size());
		getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[2].productId").isEqualTo(productId)
				.jsonPath("$[2].recommendationId").isEqualTo(3);
	}

	@Test
	void duplicateError() {
		int productId = 1;
		int recommendationId = 1;
		postAndVerifyRecommendation(productId, recommendationId, HttpStatus.OK)
				.jsonPath("$.productId").isEqualTo(productId)
				.jsonPath("$.recommendationId").isEqualTo(recommendationId);
		assertEquals(1, repository.count());
		postAndVerifyRecommendation(productId, recommendationId, HttpStatus.UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/recommendation")
				.jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1," +
						" Recommendation Id: 1");
		assertEquals(1, repository.count());
	}

	@Test
	void getRecommendationsMissingParameter() {
		getAndVerifyRecommendationsByProductId("", HttpStatus.BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/recommendation")
				.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	void getRecommendationsInvalidParameter() {
		getAndVerifyRecommendationsByProductId("?productId=no-integer", HttpStatus.BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/recommendation")
				.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	void getRecommendationsNotFound() {
		int productIdNotFound = 113;
		getAndVerifyRecommendationsByProductId(102, HttpStatus.OK)
				.jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	void getRecommendationsInvalidParameterNegativeValue() {
		int productIdInvalid = -1;
		getAndVerifyRecommendationsByProductId(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/recommendation")
				.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(
			String productIdQuery, HttpStatus expectedStatus
	) {
		return client.get()
				.uri("/recommendation" + productIdQuery)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(
			int productId, HttpStatus expectedStatus
	) {
		return getAndVerifyRecommendationsByProductId(
				"?productId="+productId,
				expectedStatus);
	}

	private WebTestClient.BodyContentSpec postAndVerifyRecommendation(
			int productId, int recommendationId, HttpStatus expectedStatus
	) {
		Recommendation recommendation = new Recommendation(productId,
				recommendationId, "Author " + recommendationId, recommendationId,
				"Content " + recommendationId, "SA");
		return client.post()
				.uri("/recommendation")
				.body(just(recommendation), Recommendation.class)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(
			int productId, HttpStatus expectedStatus
	) {
		return client.delete()
				.uri("/recommendation?productId=" + productId)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}
}
