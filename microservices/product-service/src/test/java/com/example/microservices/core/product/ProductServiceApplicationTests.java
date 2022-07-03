package com.example.microservices.core.product;

import static org.junit.jupiter.api.Assertions.*;
import static reactor.core.publisher.Mono.just;

import com.example.api.core.product.Product;
import com.example.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests extends MongoDbTestBase {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    void getProductById() {
        int productId = 1;
		assertNull(repository.findByProductId(productId).block());
		assertEquals(0, (long)repository.count().block());
		postAndVerifyProduct(productId, HttpStatus.OK);
		assertNotNull(repository.findByProductId(productId).block());
		assertEquals(1, (long)repository.count().block());
		getAndVerifyProduct(productId, HttpStatus.OK)
				.jsonPath("$.productId").isEqualTo(productId);
    }

	@Test
	void duplicateError() {
		int productId = 1;
		assertNull(repository.findByProductId(productId).block());
		postAndVerifyProduct(productId, HttpStatus.OK);
		assertNotNull(repository.findByProductId(productId).block());
		postAndVerifyProduct(productId, HttpStatus.UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/product")
				.jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
	}

	@Test
	void deleteProduct() {
		int productId = 1;
		postAndVerifyProduct(productId, HttpStatus.OK);
		assertNotNull(repository.findByProductId(productId).block());
		deleteAndVerifyProduct(productId, HttpStatus.OK);
		assertNull(repository.findByProductId(productId).block());

		deleteAndVerifyProduct(productId, HttpStatus.OK);
	}


    @Test
    void getProductInvalidParameterString() {
       getAndVerifyProduct("/no-integer", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/product/no-integer")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    void getProductNotFound() {
        int productIdNotFound = 13;
        getAndVerifyProduct(productIdNotFound, HttpStatus.NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }

    @Test
    void getProductInvalidParameterNegativeValue() {
        int productIdInvalid = -1;
        getAndVerifyProduct(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec postAndVerifyProduct(
            int productId,
            HttpStatus expectedStatus) {
		Product product = new Product(productId, "Name " + productId,
				productId, "SA");
		return client.post()
				.uri("/product")
				.body(just(product), Product.class)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody();

    }

	private void deleteAndVerifyProduct(
			int productId,
			HttpStatus expectedStatus
	) {
		client.delete()
				.uri("/product/" + productId)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(
			String productIdPath,
			HttpStatus expectedStatus
	) {
		return client.get()
				.uri("/product" + productIdPath)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(
			int productId,
			HttpStatus expectedStatus
	) {
		return getAndVerifyProduct("/" + productId, expectedStatus);
	}
}
