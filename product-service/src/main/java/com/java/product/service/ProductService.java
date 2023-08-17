package com.java.product.service;

import com.java.product.dto.ProductRequest;
import com.java.product.dto.ProductResponse;
import com.java.product.model.Product;
import com.java.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public void createProduct(ProductRequest productRequest){
        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .build();
        productRepository.save(product);
        log.info("Product {} created successfully", product.getId());
    }

    public List<ProductResponse> getAllProducts(){
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::mapToProductResponse).toList();// method reference (replaced lambda)
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }
}
