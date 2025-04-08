package dev.danvega.courses.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(
        name = "getSellerProducts",
        description = "Retrieves all products belonging to a specific seller. Returns an array of product objects with details like name, price, and stock quantity."
    )
    public List<Product> getSellerProducts(@ToolParam(description = "The unique identifier of the seller") String sellerId) {
        validateSeller(sellerId);

        return productTable.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(product -> product.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    @Tool(
        name = "getProduct",
        description = "Retrieves a specific product by its ID and seller ID. Returns detailed information about the product including price, stock, and specifications."
    )
    public Product getProduct(
            @ToolParam(description = "The unique identifier of the product") String id,
            @ToolParam(description = "The unique identifier of the seller who owns the product") String sellerId) {
        validateSeller(sellerId);

        Product product = productTable.getItem(Key.builder()
                .partitionValue(id)
                .sortValue(sellerId)
                .build());
                
        validateProductOwnership(product, sellerId);
        return product;
    }

    @Tool(
        name = "addProduct",
        description = "Adds a new product to a seller's inventory. Requires product details like name, description, price, and stock quantity. Returns the created product with a generated ID."
    )
    public Product addProduct(
            @ToolParam(description = "The unique identifier of the seller who will own the product") String sellerId,
            @ToolParam(description = "The product object containing name, description, price, stockQuantity, and other details") Product product) {
        validateSeller(sellerId);

        product.setId(UUID.randomUUID().toString());
        product.setSellerId(sellerId);
        product.setActive(true);
        product.setCreatedAt(java.time.Instant.now().toString());
        product.setUpdatedAt(product.getCreatedAt());
        
        productTable.putItem(product);
        return product;
    }

    @Tool(
        name = "updateProduct",
        description = "Updates an existing product's information. Requires the product ID, seller ID, and updated product details. Returns the updated product object."
    )
    public Product updateProduct(
            @ToolParam(description = "The unique identifier of the product to update") String id,
            @ToolParam(description = "The unique identifier of the seller who owns the product") String sellerId,
            @ToolParam(description = "The updated product object with new information") Product product) {
        validateSeller(sellerId);
        validateProductOwnership(getProduct(id, sellerId), sellerId);

        product.setId(id);
        product.setSellerId(sellerId);
        product.setUpdatedAt(java.time.Instant.now().toString());
        
        productTable.putItem(product);
        return product;
    }

    @Tool(
        name = "deleteProduct",
        description = "Permanently removes a product from a seller's inventory. This action cannot be undone."
    )
    public void deleteProduct(
            @ToolParam(description = "The unique identifier of the product to delete") String id,
            @ToolParam(description = "The unique identifier of the seller who owns the product") String sellerId) {
        validateSeller(sellerId);
        validateProductOwnership(getProduct(id, sellerId), sellerId);

        productTable.deleteItem(Key.builder()
                .partitionValue(id)
                .sortValue(sellerId)
                .build());
    }

    @Tool(
        name = "updateStock",
        description = "Updates the stock quantity of a product. Useful for inventory management after sales or restocking."
    )
    public Product updateStock(
            @ToolParam(description = "The unique identifier of the product") String id,
            @ToolParam(description = "The unique identifier of the seller who owns the product") String sellerId,
            @ToolParam(description = "The new stock quantity to set for the product") int newQuantity) {
        validateSeller(sellerId);
        Product product = getProduct(id, sellerId);
        validateProductOwnership(product, sellerId);

        product.setStockQuantity(newQuantity);
        product.setUpdatedAt(java.time.Instant.now().toString());
        productTable.putItem(product);
        return product;
    }

    @Tool(
        name = "checkLowStock",
        description = "Identifies products with stock quantity below their reorder point. Useful for inventory alerts and restocking decisions."
    )
    public List<Product> checkLowStock(@ToolParam(description = "The unique identifier of the seller") String sellerId) {
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