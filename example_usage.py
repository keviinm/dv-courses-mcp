from llm_mcp_client import LLMMCPClient
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

def main():
    # Create an instance of the LLM MCP client
    client = LLMMCPClient()
    
    # Example 1: List all sellers
    print("\n=== Example 1: List all sellers ===")
    result = client.execute_command("Show me all sellers")
    print(result)
    
    # Example 2: Create a new seller
    print("\n=== Example 2: Create a new seller ===")
    result = client.execute_command(
        "Create a new seller named Acme Corporation with email acme@example.com"
    )
    print(result)
    
    # Extract the seller ID from the response
    # This is a simplified example - in a real application, you'd parse the JSON response
    seller_id = "extracted_id_from_response"  # In reality, you'd extract this from the response
    
    # Example 3: Add a product to the seller
    print("\n=== Example 3: Add a product to the seller ===")
    result = client.execute_command(
        f"Add a product called Widget to seller with ID {seller_id}. "
        f"The product costs 29.99, has 100 units in stock, and is a high-quality widget."
    )
    print(result)
    
    # Example 4: Get seller details
    print("\n=== Example 4: Get seller details ===")
    result = client.execute_command(f"What are the details for seller with ID {seller_id}?")
    print(result)
    
    # Example 5: Get products for the seller
    print("\n=== Example 5: Get products for the seller ===")
    result = client.execute_command(f"What products does seller with ID {seller_id} have?")
    print(result)

if __name__ == "__main__":
    main() 