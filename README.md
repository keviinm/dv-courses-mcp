# Product Management System

This is a Spring Boot application that demonstrates a generic product management system with seller support. The system uses AWS DynamoDB for data storage and provides RESTful APIs for managing products and sellers.

## Features

- Seller Management (CRUD operations)
- Product Management (CRUD operations)
- Stock Management
- Low Stock Alerts
- Multi-seller Support
- Flexible Product Specifications

## Project Structure

- `model/`
  - `Product.java`: Product entity with DynamoDB annotations
  - `Seller.java`: Seller entity with DynamoDB annotations
- `service/`
  - `ProductService.java`: Service for product management
  - `SellerService.java`: Service for seller management
- `controller/`
  - `ProductController.java`: REST controller for product endpoints
  - `SellerController.java`: REST controller for seller endpoints

## API Endpoints

### Seller Management
```
GET    /api/sellers              - Get all sellers
GET    /api/sellers/{id}         - Get a seller
POST   /api/sellers              - Add a new seller
PUT    /api/sellers/{id}         - Update a seller
DELETE /api/sellers/{id}         - Delete a seller
PATCH  /api/sellers/{id}/deactivate - Deactivate a seller
```

### Product Management
```
GET    /api/sellers/{sellerId}/products          - Get seller's products
GET    /api/sellers/{sellerId}/products/{id}     - Get specific product
POST   /api/sellers/{sellerId}/products          - Add new product
PUT    /api/sellers/{sellerId}/products/{id}     - Update product
DELETE /api/sellers/{sellerId}/products/{id}     - Delete product
PATCH  /api/sellers/{sellerId}/products/{id}/stock - Update stock
GET    /api/sellers/{sellerId}/products/low-stock  - Get low stock products
```

## Example Usage

### Creating a Seller
```bash
curl -X POST http://localhost:8080/api/sellers \
-H "Content-Type: application/json" \
-d '{
    "name": "Tech Store",
    "email": "tech@store.com",
    "phone": "+1234567890",
    "address": "123 Tech Street",
    "businessType": "Retail",
    "taxId": "TAX123456"
}'
```

### Adding a Product
```bash
curl -X POST http://localhost:8080/api/sellers/{sellerId}/products \
-H "Content-Type: application/json" \
-d '{
    "name": "Product Name",
    "category": "Category",
    "brand": "Brand",
    "description": "Description",
    "price": 99.99,
    "stockQuantity": 100,
    "specifications": "{\"key\": \"value\"}",
    "imageUrl": "https://example.com/image.jpg",
    "sku": "SKU123",
    "barcode": "123456789",
    "costPrice": 80.00,
    "unitOfMeasure": "piece",
    "reorderPoint": 10,
    "reorderQuantity": 50,
    "location": "Warehouse A"
}'
```

## Getting Started

1. Clone the repository
2. Configure AWS credentials
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Access the API at `http://localhost:8080`

## Technologies Used

- Spring Boot 3.x
- AWS DynamoDB
- Maven
- Java 17