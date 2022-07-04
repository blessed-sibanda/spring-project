package com.example.microservices.composite.product.services;

import com.example.api.composite.product.*;
import com.example.api.core.product.Product;
import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.review.Review;
import com.example.api.exceptions.NotFoundException;
import com.example.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
    private static final Logger LOG =
            LoggerFactory.getLogger(ProductCompositeServiceImpl.class);
    private final ServiceUtil serviceUtil;

    private final ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(
            ServiceUtil serviceUtil,
            ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public Mono<ProductAggregate> getProduct(int productId) {
//        LOG.debug("getCompositeProduct: lookup a product aggregate for productId: {}", productId);
//        Product product = integration.getProduct(productId);
//        if (product == null)
//            throw new NotFoundException("No product found for productId: " + productId);
//        List<Recommendation> recommendations = integration.getRecommendations(productId);
//        List<Review> reviews = integration.getReviews(productId);
//        LOG.debug("getCompositeProduct: aggregate entity found for productId: {}", productId);
//        return createProductAggregate(product, recommendations, reviews,
//                serviceUtil.getServiceAddress());
        return Mono.zip(
                values -> createProductAggregate(
                        (Product) values[0],
                        (List<Recommendation>) values[1],
                        (List<Review>) values[2],
                        serviceUtil.getServiceAddress()
                ),
                integration.getProduct(productId),
                integration.getRecommendations(productId).collectList(),
        )
    }

    @Override
    public void createProduct(ProductAggregate body) {
        LOG.debug("Body -->: {}", body);
        try {
            Product product = new Product(body.getProductId(),
                    body.getName(), body.getWeight(), null);
            integration.createProduct(product);
            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(
                            body.getProductId(),
                            r.getRecommendationId(),
                            r.getAuthor(),
                            r.getRate(),
                            r.getContent(),
                            null);
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(),
                            r.getReviewId(), r.getAuthor(),
                            r.getSubject(), r.getContent(), null);
                    integration.createReview(review);
                });
            }
        } catch (RuntimeException re) {
            LOG.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    @Override
    public void deleteProduct(int productId) {
        integration.deleteProduct(productId);
        integration.deleteRecommendations(productId);
        integration.deleteReviews(productId);
    }

    private ProductAggregate createProductAggregate(
            Product product,
            List<Recommendation> recommendations,
            List<Review> reviews,
            String serviceAddress
    ) {
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        List<RecommendationSummary> recommendationSummaries =
                (recommendations == null) ? null :
                        recommendations.stream().map(r ->
                                new RecommendationSummary(r.getRecommendationId(),
                                        r.getAuthor(),
                                        r.getRate(),
                                        r.getContent())).collect(Collectors.toList());

        List<ReviewSummary> reviewSummaries =
                (reviews == null) ? null :
                        reviews.stream().map(r ->
                                new ReviewSummary(r.getReviewId(),
                                        r.getAuthor(),
                                        r.getSubject(), r.getContent())).collect(Collectors.toList());

        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ?
                reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ?
                recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(
                serviceAddress, productAddress, reviewAddress, recommendationAddress
        );
        return new ProductAggregate(productId, name, weight,
                recommendationSummaries,
                reviewSummaries, serviceAddresses);

    }
}
