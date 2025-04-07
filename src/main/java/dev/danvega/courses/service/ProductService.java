package dev.danvega.courses.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.danvega.courses.model.Product;
import dev.danvega.courses.model.Seller;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String TABLE_NAME = "Products";
    
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<Product> productTable;
    private final SellerService sellerService;

    public ProductService(DynamoDbEnhancedClient dynamoDbEnhancedClient, SellerService sellerService) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.productTable = dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(Product.class));
        this.sellerService = sellerService;
    }

    // MCP Tool: Get all products for a seller
    public List<Product> getSellerProducts(String sellerId) {
        validateSeller(sellerId);

        return productTable.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(product -> product.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    // MCP Tool: Get a specific product
    public Product getProduct(String id, String sellerId) {
        validateSeller(sellerId);

        Product product = productTable.getItem(Key.builder()
                .partitionValue(id)
                .sortValue(sellerId)
                .build());
                
        validateProductOwnership(product, sellerId);
        return product;
    }

    // MCP Tool: Add a new product
    public Product addProduct(String sellerId, Product product) {
        validateSeller(sellerId);

        product.setId(UUID.randomUUID().toString());
        product.setSellerId(sellerId);
        product.setActive(true);
        product.setCreatedAt(java.time.Instant.now().toString());
        product.setUpdatedAt(product.getCreatedAt());
        
        productTable.putItem(product);
        return product;
    }

    // MCP Tool: Update an existing product
    public Product updateProduct(String id, String sellerId, Product product) {
        validateSeller(sellerId);
        validateProductOwnership(getProduct(id, sellerId), sellerId);

        product.setId(id);
        product.setSellerId(sellerId);
        product.setUpdatedAt(java.time.Instant.now().toString());
        
        productTable.putItem(product);
        return product;
    }

    // MCP Tool: Delete a product
    public void deleteProduct(String id, String sellerId) {
        validateSeller(sellerId);
        validateProductOwnership(getProduct(id, sellerId), sellerId);

        productTable.deleteItem(Key.builder()
                .partitionValue(id)
                .sortValue(sellerId)
                .build());
    }

    // MCP Tool: Update stock quantity
    public Product updateStock(String id, String sellerId, int newQuantity) {
        validateSeller(sellerId);
        Product product = getProduct(id, sellerId);
        validateProductOwnership(product, sellerId);

        product.setStockQuantity(newQuantity);
        product.setUpdatedAt(java.time.Instant.now().toString());
        productTable.putItem(product);
        return product;
    }

    // MCP Tool: Check for low stock products
    public List<Product> checkLowStock(String sellerId) {
        validateSeller(sellerId);

        return getSellerProducts(sellerId).stream()
                .filter(product -> product.getStockQuantity() <= product.getReorderPoint())
                .collect(Collectors.toList());
    }

    private void validateSeller(String sellerId) {
        Seller seller = sellerService.getSeller(sellerId);
        if (seller == null || !seller.isActive()) {
            throw new IllegalArgumentException("Invalid or inactive seller");
        }
    }

    private void validateProductOwnership(Product product, String sellerId) {
        if (product != null && !product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Product does not belong to the seller");
        }
    }

    @PostConstruct
    public void init() {
        try {
            productTable.createTable();
        } catch (Exception e) {
            log.info("Table already exists or error creating table: {}", e.getMessage());
        }
    }
} 