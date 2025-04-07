package dev.danvega.courses.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.danvega.courses.model.Product;
import dev.danvega.courses.service.ProductService;
import dev.danvega.courses.service.SellerService;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final SellerService sellerService;

    public ProductController(ProductService productService, SellerService sellerService) {
        this.productService = productService;
        this.sellerService = sellerService;
    }

    @GetMapping("/sellers/{sellerId}/products")
    public ResponseEntity<List<Product>> getSellerProducts(@PathVariable String sellerId) {
        try {
            List<Product> products = productService.getSellerProducts(sellerId);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/sellers/{sellerId}/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String sellerId, @PathVariable String id) {
        try {
            Product product = productService.getProduct(id, sellerId);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/sellers/{sellerId}/products")
    public ResponseEntity<Product> addProduct(@PathVariable String sellerId, @RequestBody Product product) {
        try {
            Product newProduct = productService.addProduct(sellerId, product);
            return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/sellers/{sellerId}/products/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String sellerId,
            @PathVariable String id,
            @RequestBody Product product) {
        try {
            Product updatedProduct = productService.updateProduct(id, sellerId, product);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/sellers/{sellerId}/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String sellerId, @PathVariable String id) {
        try {
            productService.deleteProduct(id, sellerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PatchMapping("/sellers/{sellerId}/products/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable String sellerId,
            @PathVariable String id,
            @RequestParam int quantity) {
        try {
            Product product = productService.updateStock(id, sellerId, quantity);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/sellers/{sellerId}/products/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(@PathVariable String sellerId) {
        try {
            List<Product> products = productService.checkLowStock(sellerId);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
} 