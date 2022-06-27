package com.example.microservices.core.product.services;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.exceptions.InvalidInputException;
import com.example.api.exceptions.NotFoundException;
import com.example.microservices.core.product.persistence.ProductEntity;
import com.example.microservices.core.product.persistence.ProductRepository;
import com.example.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ProductRepository repository,
                              ProductMapper mapper,
                              ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product getProduct(int productId) {
        LOG.debug("/product return the found product for productId={}", productId);
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        ProductEntity entity = repository.findByProductId(productId)
                .orElseThrow(() ->
                        new NotFoundException("No product found for productId: " + productId));
        Product response = mapper.entityToApi(entity);
        response.setServiceAddress(serviceUtil.getServiceAddress());
        LOG.debug("getProduct: found productId: {}", response.getProductId());
        return response;
    }

    @Override
    public Product createProduct(Product body) {
        try {
            ProductEntity entity = mapper.apiToEntity(body);
            ProductEntity newEntity = repository.save(entity);
            return mapper.entityToApi(newEntity);
        } catch (DuplicateKeyException dke) {
            throw new InvalidInputException("Duplicate key, Product Id: " +
                    body.getProductId());
        }
    }

    @Override
    public void deleteProduct(int productId) {
        repository.findByProductId(productId)
                .ifPresent(repository::delete);
    }
}
