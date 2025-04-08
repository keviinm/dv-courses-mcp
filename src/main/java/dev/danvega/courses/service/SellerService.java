package dev.danvega.courses.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(
        name = "getAllSellers",
        description = "Retrieves a list of all sellers in the system. Returns an array of seller objects with their details including ID, name, email, and active status."
    )
    public List<Seller> getAllSellers() {
        return sellerTable.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    @Tool(
        name = "getSeller",
        description = "Retrieves a specific seller by their unique ID. Returns detailed information about the seller including contact details and active status."
    )
    public Seller getSeller(@ToolParam(description = "The unique identifier of the seller") String id) {
        return sellerTable.getItem(Key.builder().partitionValue(id).build());
    }

    @Tool(
        name = "addSeller",
        description = "Creates a new seller in the system. Requires seller details like name and email. Returns the created seller with a generated ID."
    )
    public Seller addSeller(@ToolParam(description = "The seller object containing name, email, phone, address, businessType, and taxId") Seller seller) {
        seller.setId(UUID.randomUUID().toString());
        seller.setActive(true);
        sellerTable.putItem(seller);
        return seller;
    }

    @Tool(
        name = "updateSeller",
        description = "Updates an existing seller's information. Requires the seller ID and updated seller details. Returns the updated seller object."
    )
    public Seller updateSeller(
            @ToolParam(description = "The unique identifier of the seller to update") String id,
            @ToolParam(description = "The updated seller object with new information") Seller seller) {
        seller.setId(id);
        sellerTable.putItem(seller);
        return seller;
    }

    @Tool(
        name = "deleteSeller",
        description = "Permanently removes a seller from the system. This action cannot be undone."
    )
    public void deleteSeller(@ToolParam(description = "The unique identifier of the seller to delete") String id) {
        sellerTable.deleteItem(Key.builder().partitionValue(id).build());
    }

    @Tool(
        name = "deactivateSeller",
        description = "Deactivates a seller by setting their active status to false. Deactivated sellers cannot add or manage products."
    )
    public Seller deactivateSeller(@ToolParam(description = "The unique identifier of the seller to deactivate") String id) {
        Seller seller = getSeller(id);
        if (seller != null) {
            seller.setActive(false);
            sellerTable.putItem(seller);
        }
        return seller;
    }

    @Tool(
        name = "createSeller",
        description = "Creates a new seller in the system. Requires seller details like name, email, phone, address, business type, tax ID, and active status. Returns the created seller with a generated ID."
    )
    public void createSeller(@ToolParam(description = "The name of the seller") String name,
                           @ToolParam(description = "The email of the seller") String email,
                           @ToolParam(description = "The phone number of the seller") String phoneNumber,
                           @ToolParam(description = "The address of the seller") String address,
                           @ToolParam(description = "The business type of the seller") String businessType,
                           @ToolParam(description = "The tax ID of the seller") String taxId,
                           @ToolParam(description = "Whether the seller is active") boolean active) {
        Seller seller = new Seller();
        seller.setId(UUID.randomUUID().toString());
        seller.setName(name);
        seller.setEmail(email);
        seller.setPhone(phoneNumber);
        seller.setAddress(address);
        seller.setBusinessType(businessType);
        seller.setTaxId(taxId);
        seller.setActive(active);
        sellerTable.putItem(seller);
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