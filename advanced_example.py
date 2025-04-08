from llm_mcp_client import LLMMCPClient
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

def main():
    # Create an instance of the LLM MCP client
    client = LLMMCPClient()
    
    # Example 1: Analytical query - Count active sellers
    print("\n=== Example 1: Count active sellers ===")
    result = client.execute_command("How many active sellers do we have?")
    print(result)
    
    # Example 2: Analytical query - Find seller with most products
    print("\n=== Example 2: Find seller with most products ===")
    result = client.execute_command("Which seller has the most products?")
    print(result)
    
    # Example 3: Analytical query - Calculate average product price
    print("\n=== Example 3: Calculate average product price ===")
    result = client.execute_command("What is the average price of all products?")
    print(result)
    
    # Example 4: Analytical query - Find low stock products
    print("\n=== Example 4: Find low stock products ===")
    result = client.execute_command("How many products have low stock (less than 10 units)?")
    print(result)
    
    # Example 5: Analytical query - Summarize seller performance
    print("\n=== Example 5: Summarize seller performance ===")
    result = client.execute_command("Summarize the performance of all sellers based on their product count and active status")
    print(result)
    
    # Example 6: Create a new seller and then analyze
    print("\n=== Example 6: Create a seller and analyze ===")
    result = client.execute_command(
        "Create a new seller named Tech Solutions with email tech@example.com"
    )
    print(result)
    
    # Now ask for an updated count
    print("\n=== Example 6b: Updated seller count ===")
    result = client.execute_command("Now how many active sellers do we have?")
    print(result)

if __name__ == "__main__":
    main() 