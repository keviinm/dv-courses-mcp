package dev.danvega.courses.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.danvega.courses.model.Seller;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

@Service
public class SellerService {

    private static final Logger log = LoggerFactory.getLogger(SellerService.class);
    private static final String TABLE_NAME = "Sellers";
    
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<Seller> sellerTable;

    public SellerService(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.sellerTable = dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(Seller.class));
    }

    // MCP Tool: Get all sellers
    public List<Seller> getAllSellers() {
        return sellerTable.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    // MCP Tool: Get a seller by ID
    public Seller getSeller(String id) {
        return sellerTable.getItem(Key.builder().partitionValue(id).build());
    }

    // MCP Tool: Add a new seller
    public Seller addSeller(Seller seller) {
        seller.setId(UUID.randomUUID().toString());
        seller.setActive(true);
        sellerTable.putItem(seller);
        return seller;
    }

    // MCP Tool: Update an existing seller
    public Seller updateSeller(String id, Seller seller) {
        seller.setId(id);
        sellerTable.putItem(seller);
        return seller;
    }

    // MCP Tool: Delete a seller
    public void deleteSeller(String id) {
        sellerTable.deleteItem(Key.builder().partitionValue(id).build());
    }

    // MCP Tool: Deactivate a seller
    public Seller deactivateSeller(String id) {
        Seller seller = getSeller(id);
        if (seller != null) {
            seller.setActive(false);
            sellerTable.putItem(seller);
        }
        return seller;
    }

    @PostConstruct
    public void init() {
        try {
            sellerTable.createTable();
        } catch (Exception e) {
            log.info("Table already exists or error creating table: {}", e.getMessage());
        }

        // Add sample sellers if the table is empty
        if (getAllSellers().isEmpty()) {
            Seller seller1 = new Seller();
            seller1.setId(UUID.randomUUID().toString());
            seller1.setName("Tech Store");
            seller1.setEmail("tech@store.com");
            seller1.setPhone("+1234567890");
            seller1.setAddress("123 Tech Street, Silicon Valley");
            seller1.setBusinessType("Retail");
            seller1.setTaxId("TAX123456");
            seller1.setActive(true);
            
            Seller seller2 = new Seller();
            seller2.setId(UUID.randomUUID().toString());
            seller2.setName("Gaming Gear");
            seller2.setEmail("info@gaminggear.com");
            seller2.setPhone("+0987654321");
            seller2.setAddress("456 Game Avenue, Gaming City");
            seller2.setBusinessType("Online Retail");
            seller2.setTaxId("TAX789012");
            seller2.setActive(true);
            
            sellerTable.putItem(seller1);
            sellerTable.putItem(seller2);
        }
    }
} 