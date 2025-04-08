from mcp_client import MCPClient

def test_interactive_conversation():
    client = MCPClient()
    
    # Test 1: Create seller with incomplete information
    print("\nTest 1: Creating a seller with incomplete information")
    print("User: Create a seller")
    result = client.process_natural_language("Create a seller")
    print(f"Assistant: {result}")
    
    print("\nUser: The name is John Doe")
    result = client.process_natural_language("The name is John Doe")
    print(f"Assistant: {result}")
    
    print("\nUser: The email is john.doe@example.com")
    result = client.process_natural_language("The email is john.doe@example.com")
    print(f"Result: {result}")
    
    # Test 2: Add product with incomplete information
    print("\nTest 2: Adding a product with incomplete information")
    print("User: Add a product")
    result = client.process_natural_language("Add a product")
    print(f"Assistant: {result}")
    
    print("\nUser: The name is Gaming Mouse")
    result = client.process_natural_language("The name is Gaming Mouse")
    print(f"Assistant: {result}")
    
    print("\nUser: The price is 49.99")
    result = client.process_natural_language("The price is 49.99")
    print(f"Assistant: {result}")
    
    print("\nUser: The stock is 50")
    result = client.process_natural_language("The stock is 50")
    print(f"Result: {result}")

if __name__ == "__main__":
    test_interactive_conversation() 