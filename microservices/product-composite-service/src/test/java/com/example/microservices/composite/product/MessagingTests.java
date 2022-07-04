package com.example.microservices.composite.product;

import com.example.api.composite.product.ProductAggregate;
import com.example.api.composite.product.RecommendationSummary;
import com.example.api.composite.product.ReviewSummary;
import com.example.api.core.product.Product;
import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.review.Review;
import com.example.api.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
public class MessagingTests {
    private static final Logger LOG = LoggerFactory.getLogger(MessagingTests.class);

    @Autowired
    private WebTestClient client;

    @Autowired
    private OutputDestination target;

    @BeforeEach
    void setUp() {
        purgeMessages("products");
        purgeMessages("recommendations");
        purgeMessages("reviews");
    }

    @Test
    void createCompositeProduct() {
        ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
        postAndVerifyProduct(composite, HttpStatus.ACCEPTED);

        final List<String> productMessages = getMessages("products");
        final List<String> recommendationMessages = getMessages("recommendations");
        final List<String> reviewMessages = getMessages("reviews");

        // Assert one expected new product event queued up
        assertEquals(1, productMessages.size());

        Event<Integer, Product> expectedEvent =
                new Event<>(Event.Type.CREATE, composite.getProductId(),
                        new Product(composite.getProductId(),
                                composite.getName(), composite.getWeight(), null));
        assertThat(productMessages.get(0), is(IsSameEvent.sameEventExceptCreatedAt(expectedEvent)));

        // assert no recommendation and review events
        assertEquals(0, recommendationMessages.size());
        assertEquals(0, reviewMessages.size());
    }


    @Test
    void createCompositeProduct2() {
        ProductAggregate compositeProduct = new ProductAggregate(1, "name",
                1, Collections.singletonList(new RecommendationSummary(1, "a", 1, "c")),
                Collections.singletonList(new ReviewSummary(1,"a", "s", "c")),
                null);
        postAndVerifyProduct(compositeProduct, HttpStatus.ACCEPTED);

        final List<String> productMessages = getMessages("products");
        final List<String> recommendationMessages = getMessages("recommendations");
        final List<String> reviewMessages = getMessages("reviews");

        // Assert one create product event queued up
        assertEquals(1, productMessages.size());

        Event<Integer, Product> expectedProductEvent =
                new Event(Event.Type.CREATE,
                        compositeProduct.getProductId(),
                        new Product(compositeProduct.getProductId(),
                                compositeProduct.getName(), compositeProduct.getWeight(), null));
        assertThat(productMessages.get(0), is(IsSameEvent.sameEventExceptCreatedAt(expectedProductEvent)));

        // Assert one create recommendation event queued up
        assertEquals(1, recommendationMessages.size());

        RecommendationSummary rec = compositeProduct.getRecommendations().get(0);
        Event<Integer, Product> expectedRecommendationEvent =
                new Event(Event.Type.CREATE, compositeProduct.getProductId(),
                        new Recommendation(compositeProduct.getProductId(),
                                rec.getRecommendationId(),
                                rec.getAuthor(), rec.getRate(),
                                rec.getContent(), null));
        assertThat(recommendationMessages.get(0),
                is(IsSameEvent.sameEventExceptCreatedAt(expectedRecommendationEvent)));

        // Assert one create review event queued up
        assertEquals(1, reviewMessages.size());

        ReviewSummary rev = compositeProduct.getReviews().get(0);
        Event<Integer, Product> expectedReviewEvent =
                new Event(Event.Type.CREATE, compositeProduct.getProductId(),
                        new Review(compositeProduct.getProductId(),
                                rev.getReviewId(), rev.getAuthor(),
                                rev.getSubject(), rev.getContent(), null));
        assertThat(reviewMessages.get(0),
                is(IsSameEvent.sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    @Test
    void deleteCompositeProduct() {
        deleteAndVerifyProduct(1, HttpStatus.ACCEPTED);

        final List<String> productMessages = getMessages("products");
        final List<String> recommendationMessages = getMessages("recommendations");
        final List<String> reviewMessages = getMessages("reviews");

        // Assert one delete product event queued up
        assertEquals(1, productMessages.size());

        Event<Integer, Product> expectedProductEvent = new Event(Event.Type.DELETE, 1, null);
        assertThat(productMessages.get(0), is(IsSameEvent.sameEventExceptCreatedAt(expectedProductEvent)));

        // Assert one delete recommendation event queued up
        assertEquals(1, recommendationMessages.size());

        Event<Integer, Product> expectedRecommendationEvent = new Event(Event.Type.DELETE, 1, null);
        assertThat(recommendationMessages.get(0), is(IsSameEvent.sameEventExceptCreatedAt(expectedRecommendationEvent)));

        // Assert one delete review event queued up
        assertEquals(1, reviewMessages.size());

        Event<Integer, Product> expectedReviewEvent = new Event(Event.Type.DELETE, 1, null);
        assertThat(reviewMessages.get(0), is(IsSameEvent.sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    private Message<byte[]> getMessage(String bindingName) {
        try {
            return target.receive(0, bindingName);
        } catch (NullPointerException npe) {
            // If the messageQueues member variable in the target object contains
            // no queues when the receive method is called, it will cause a NPE to
            // thrown. So we catch the NPE here and return null to indicate that no
            // messages were found
            LOG.error("getMessage() received a NPE with binding = {}", bindingName);
            return null;
        }
    }

    private List<String> getMessages(String bindingName) {
        List<String> messages = new ArrayList<>();
        boolean anyMoreMessages = true;

        while (anyMoreMessages) {
            Message<byte[]> message = getMessage(bindingName);
            if (message == null ) {
                anyMoreMessages=false;
            } else {
                messages.add(new String(message.getPayload()));
            }
        }
        return messages;
    }

    private  void purgeMessages(String bindingName) {
        getMessages(bindingName);
    }

    private void postAndVerifyProduct(ProductAggregate compositeProduct,
                                 HttpStatus expectedStatus) {
        client.post()
                .uri("/product-composite")
                .body(Mono.just(compositeProduct), ProductAggregate.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/product-composite/" + productId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

}
