package com.example.microservices.composite.product.services;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.recommendation.RecommendationService;
import com.example.api.core.review.Review;
import com.example.api.core.review.ReviewService;
import com.example.api.event.Event;
import com.example.api.exceptions.InvalidInputException;
import com.example.api.exceptions.NotFoundException;
import com.example.util.http.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.util.logging.Level;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);
    private final ObjectMapper mapper;
    private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;
    private final WebClient webClient;

    private static final String PRODUCT_SERVICE_URL = "http://product";
    private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
    private static final String REVIEW_SERVICE_URL = "http://review";

    @Autowired
    public ProductCompositeIntegration(
            @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
            WebClient.Builder webClientBuilder,
            ObjectMapper mapper,
            StreamBridge streamBridge) {
        this.publishEventScheduler = publishEventScheduler;
        this.webClient = webClientBuilder.build();
        this.mapper = mapper;
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        String url = PRODUCT_SERVICE_URL + "/product/" + productId;
        return webClient.get().uri(url).retrieve()
                .bodyToMono(Product.class)
                .log(LOG.getName(), Level.FINE)
                .onErrorMap(WebClientResponseException.class,
                        ex -> handleException(ex));
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        return Mono.fromCallable(() -> {
            sendMessage("products-out-0", new Event(Event.Type.CREATE,
                    body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        return Mono.fromRunnable(() -> sendMessage("products-out-0",
                        new Event(Event.Type.DELETE, productId, null)))
                .subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        String url = RECOMMENDATION_SERVICE_URL + "/recommendation?productId=" + productId;
        LOG.debug("Will call getRecommendations API on URL: {}", url);
        // Return an empty result if something goes wrong to make it possible for
        // the composite service to return partial responses
        return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class)
                .log(LOG.getName(), Level.FINE).onErrorResume(error -> Flux.empty());
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {
        return Mono.fromCallable(() -> {
            sendMessage("recommendations-out-0",
                    new Event(Event.Type.CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        return Mono.fromRunnable(() ->
                        sendMessage("recommendations-out-0",
                                new Event(Event.Type.DELETE, productId, null)))
                .subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Mono<Review> createReview(Review body) {
        return Mono.fromCallable(() -> {
            sendMessage("reviews-out-0", new Event(Event.Type.CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        String url = REVIEW_SERVICE_URL + "/review?productId=" + productId;
        LOG.debug("Will call getReviews API on URL: {}", url);
        // Return an empty result if something goes wrong to make it possible
        // for the composite service to return partial responses
        return webClient.get().uri(url).retrieve().bodyToFlux(Review.class)
                .log(LOG.getName(), Level.FINE).onErrorResume(error -> Flux.empty());
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        return Mono.fromRunnable(() -> {
            sendMessage("reviews-out-0",
                    new Event(Event.Type.DELETE, productId, null));
        }).subscribeOn(publishEventScheduler).then();
    }

    public Mono<Health> getProductHealth() {
        return getHealth(PRODUCT_SERVICE_URL);
    }

    public Mono<Health> getRecommendationHealth() {
        return getHealth(RECOMMENDATION_SERVICE_URL);
    }
    public Mono<Health> getReviewHealth() {
        return getHealth(REVIEW_SERVICE_URL);
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ioex.getMessage();
        }
    }

    private Throwable handleException(Throwable ex) {
        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got an unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }
        WebClientResponseException wcre = (WebClientResponseException) ex;
        switch (wcre.getStatusCode()) {
            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));
            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));
            default:
                LOG.warn("Got an unexpected HTTP error; {}, will rethrow it",
                        wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private void sendMessage(String bindingName, Event event) {
        LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey()).build();
        streamBridge.send(bindingName, message);
    }

    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        LOG.debug("Will call the Health API on URL: {}", url);
        return webClient.get().uri(url).retrieve().bodyToMono(String.class)
                .map(s -> new Health.Builder().up().build())
                .onErrorResume(ex -> Mono.just(
                        new Health.Builder().down(ex).build()
                ))
                .log(LOG.getName(), Level.FINE);
    }
}
