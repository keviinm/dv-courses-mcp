import requests
import json
import os
import re
from datetime import datetime
from typing import Dict, List, Optional, Union, Any, Tuple
import logging
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("mcp_client.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class ConversationEntry:
    def __init__(self, operation: str, details: Dict, timestamp: datetime = None):
        self.operation = operation
        self.details = details
        self.timestamp = timestamp or datetime.now()

    def to_dict(self) -> Dict:
        return {
            "operation": self.operation,
            "details": self.details,
            "timestamp": self.timestamp.isoformat()
        }

class MCPError(Exception):
    """Custom exception for MCP client errors with friendly messages"""
    def __init__(self, message: str, status_code: Optional[int] = None, details: Optional[Dict] = None):
        self.message = message
        self.status_code = status_code
        self.details = details or {}
        super().__init__(self.message)

class MCPClient:
    def __init__(self, base_url="http://localhost:8084", session_file="mcp_session.json"):
        self.base_url = base_url
        self.headers = {
            "Content-Type": "application/json"
        }
        self.conversation_history: List[ConversationEntry] = []
        self.current_seller: Optional[Dict] = None
        self.current_product: Optional[Dict] = None
        self.context: Dict = {}
        self.session_file = session_file
        self.pending_operation: Optional[Dict] = None
        self.session = requests.Session()
        self.conversation_state = {}
        self._load_session()

    def _load_session(self):
        """Load session data from file if it exists"""
        try:
            if os.path.exists(self.session_file):
                with open(self.session_file, 'r') as f:
                    session_data = json.load(f)
                    self.conversation_history = [
                        ConversationEntry(
                            entry["operation"], 
                            entry["details"], 
                            datetime.fromisoformat(entry["timestamp"])
                        ) for entry in session_data.get("conversation_history", [])
                    ]
                    self.current_seller = session_data.get("current_seller")
                    self.current_product = session_data.get("current_product")
                    self.context = session_data.get("context", {})
                    logger.info("Session loaded successfully")
        except Exception as e:
            logger.warning(f"Failed to load session: {str(e)}")

    def _save_session(self):
        """Save current session data to file"""
        try:
            session_data = {
                "conversation_history": [entry.to_dict() for entry in self.conversation_history],
                "current_seller": self.current_seller,
                "current_product": self.current_product,
                "context": self.context
            }
            with open(self.session_file, 'w') as f:
                json.dump(session_data, f, indent=2)
            logger.info("Session saved successfully")
        except Exception as e:
            logger.warning(f"Failed to save session: {str(e)}")

    def _add_to_history(self, operation: str, details: Dict):
        entry = ConversationEntry(operation, details)
        self.conversation_history.append(entry)
        self._save_session()
        return entry

    def _handle_response(self, response: requests.Response, operation: str) -> Dict:
        """Handle API response with friendly error messages"""
        try:
            response.raise_for_status()
            data = response.json()
            return data
        except requests.exceptions.HTTPError as e:
            error_msg = f"API Error: {str(e)}"
            try:
                error_details = response.json()
                error_msg = error_details.get("message", error_msg)
            except:
                pass
            raise MCPError(error_msg, response.status_code)
        except requests.exceptions.RequestException as e:
            raise MCPError(f"Connection Error: {str(e)}")
        except json.JSONDecodeError:
            raise MCPError("Invalid response format from server")

    def _natural_language_query(self, query: str) -> Dict[str, Any]:
        """Process natural language queries to determine intent and parameters"""
        query = query.lower().strip()
        
        # Seller-related intents
        if "create seller" in query or "add seller" in query:
            name_match = re.search(r"name\s+is\s+([^,\.]+)", query)
            email_match = re.search(r"email\s+is\s+([^,\.]+)", query)
            return {
                "intent": "create_seller",
                "params": {
                    "name": name_match.group(1) if name_match else None,
                    "email": email_match.group(1) if email_match else None
                }
            }
        
        # Product-related intents
        elif "add product" in query or "create product" in query:
            name_match = re.search(r"name\s+is\s+([^,\.]+)", query)
            price_match = re.search(r"price\s+is\s+(\d+\.?\d*)", query)
            stock_match = re.search(r"stock\s+is\s+(\d+)", query)
            return {
                "intent": "add_product",
                "params": {
                    "name": name_match.group(1) if name_match else None,
                    "price": float(price_match.group(1)) if price_match else None,
                    "stock": int(stock_match.group(1)) if stock_match else None
                }
            }
        
        # Stock-related intents
        elif "update stock" in query or "change stock" in query:
            product_match = re.search(r"product\s+([^,\.]+)", query)
            stock_match = re.search(r"to\s+(\d+)", query)
            return {
                "intent": "update_stock",
                "params": {
                    "product_name": product_match.group(1) if product_match else None,
                    "new_stock": int(stock_match.group(1)) if stock_match else None
                }
            }
        
        # Check if this is a response to a pending question
        elif self.pending_operation:
            # Handle price input
            if self.pending_operation.get("waiting_for") == "price":
                price_match = re.search(r"(\d+\.?\d*)", query)
                if price_match:
                    self.pending_operation["params"]["price"] = float(price_match.group(1))
                    self.pending_operation["waiting_for"] = "stock"
                    return self.pending_operation
            
            # Handle stock input
            elif self.pending_operation.get("waiting_for") == "stock":
                stock_match = re.search(r"(\d+)", query)
                if stock_match:
                    self.pending_operation["params"]["stock"] = int(stock_match.group(1))
                    self.pending_operation["waiting_for"] = None
                    return self.pending_operation
            
            # Handle email input
            elif self.pending_operation.get("waiting_for") == "email":
                email_match = re.search(r"([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})", query)
                if email_match:
                    self.pending_operation["params"]["email"] = email_match.group(1)
                    self.pending_operation["waiting_for"] = None
                    return self.pending_operation
        
        return {"intent": "unknown", "params": {}}

    def _ask_for_missing_details(self, intent: str, params: Dict) -> Tuple[bool, Dict]:
        """Check if any required parameters are missing and return a question if needed"""
        if intent == "create_seller":
            if not params["name"]:
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "name",
                    "question": "What is the seller's name?"
                }
            elif not params["email"]:
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "email",
                    "question": "What is the seller's email address?"
                }
        
        elif intent == "add_product":
            if not self.current_seller:
                return True, {
                    "intent": "select_seller",
                    "params": {},
                    "waiting_for": "seller_id",
                    "question": "Which seller would you like to add a product for? Please provide the seller ID."
                }
            elif not params["name"]:
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "name",
                    "question": "What is the name of the product?"
                }
            elif params["price"] is None:
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "price",
                    "question": f"What is the price for {params['name'] or 'the product'}?"
                }
            elif params["stock"] is None:
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "stock",
                    "question": f"How many units of {params['name'] or 'the product'} are in stock?"
                }
        
        elif intent == "update_stock":
            if not self.current_seller:
                return True, {
                    "intent": "select_seller",
                    "params": {},
                    "waiting_for": "seller_id",
                    "question": "Which seller's product would you like to update? Please provide the seller ID."
                }
            elif not self.current_product and not params["product_name"]:
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "product_name",
                    "question": "Which product would you like to update? Please provide the product name."
                }
            elif params["new_stock"] is None:
                product_name = params["product_name"] or (self.current_product["name"] if self.current_product else "the product")
                return True, {
                    "intent": intent,
                    "params": params,
                    "waiting_for": "new_stock",
                    "question": f"What is the new stock quantity for {product_name}?"
                }
        
        return False, {"intent": intent, "params": params}

    def process_natural_language(self, text):
        """Process natural language input and return appropriate response"""
        text = text.lower().strip()
        
        # Check if we're in the middle of a conversation
        if self.conversation_state.get('in_progress'):
            return self._handle_conversation_continuation(text)
            
        # Initial request handling
        if "create" in text and "seller" in text:
            self.conversation_state = {
                'in_progress': True,
                'action': 'create_seller',
                'collected_info': {}
            }
            return "I'll help you create a seller. What's the seller's name?"
            
        elif "add" in text and "product" in text:
            self.conversation_state = {
                'in_progress': True,
                'action': 'add_product',
                'collected_info': {}
            }
            return "I'll help you add a product. What's the product name?"
            
        return "I can help you create a seller or add a product. What would you like to do?"
        
    def _handle_conversation_continuation(self, text):
        """Handle continuation of an ongoing conversation"""
        action = self.conversation_state['action']
        collected = self.conversation_state['collected_info']
        
        if action == 'create_seller':
            if 'name' not in collected:
                # Extract name from "The name is ..." format
                name_match = re.search(r"(?:the\s+name\s+is\s+)?(.+)", text, re.IGNORECASE)
                collected['name'] = name_match.group(1) if name_match else text
                return "Great! What's the seller's email address?"
            elif 'email' not in collected:
                # Extract email from "The email is ..." format
                email_match = re.search(r"(?:the\s+email\s+is\s+)?(.+)", text, re.IGNORECASE)
                collected['email'] = email_match.group(1) if email_match else text
                try:
                    seller = self.create_seller(
                        name=collected['name'],
                        email=collected['email']
                    )
                    self.conversation_state = {}
                    return f"Seller created successfully! ID: {seller['id']}"
                except Exception as e:
                    self.conversation_state = {}
                    return f"Error creating seller: {str(e)}"
                    
        elif action == 'add_product':
            if 'name' not in collected:
                # Extract name from "The name is ..." format
                name_match = re.search(r"(?:the\s+name\s+is\s+)?(.+)", text, re.IGNORECASE)
                collected['name'] = name_match.group(1) if name_match else text
                return "What's the product price?"
            elif 'price' not in collected:
                # Extract price from "The price is ..." format
                price_match = re.search(r"(?:the\s+price\s+is\s+)?(\d+\.?\d*)", text, re.IGNORECASE)
                if price_match:
                    try:
                        price = float(price_match.group(1))
                        collected['price'] = price
                        return "How many units are in stock?"
                    except ValueError:
                        return "Please provide a valid price (e.g., 49.99)"
                else:
                    return "Please provide a valid price (e.g., 49.99)"
            elif 'stock' not in collected:
                # Extract stock from "The stock is ..." format
                stock_match = re.search(r"(?:the\s+stock\s+is\s+)?(\d+)", text, re.IGNORECASE)
                if stock_match:
                    try:
                        stock = int(stock_match.group(1))
                        collected['stock'] = stock
                        # Note: This would need a seller_id in a real implementation
                        self.conversation_state = {}
                        return "Product added successfully! (Note: Seller ID needed in real implementation)"
                    except ValueError:
                        return "Please provide a valid stock quantity (whole number)"
                else:
                    return "Please provide a valid stock quantity (whole number)"
                    
        return "I'm not sure how to process that. Let's start over."

    def _execute_intent(self, intent: str, params: Dict) -> Dict:
        """Execute the intent with the provided parameters"""
        if intent == "create_seller":
            if not params["name"] or not params["email"]:
                raise MCPError("Please provide both name and email for the seller")
            return self.create_seller(params["name"], params["email"])
        
        elif intent == "add_product":
            if not self.current_seller:
                raise MCPError("Please select a seller first")
            if not params["name"] or params["price"] is None or params["stock"] is None:
                raise MCPError("Please provide name, price, and stock for the product")
            return self.add_product(
                self.current_seller["id"],
                params["name"],
                f"Product added via natural language query",
                params["price"],
                params["stock"]
            )
        
        elif intent == "update_stock":
            if not self.current_seller:
                raise MCPError("Please select a seller first")
            if not self.current_product and not params["product_name"]:
                raise MCPError("Please select a product first")
            if params["new_stock"] is None:
                raise MCPError("Please provide the new stock quantity")
            
            # If we have a product name but no current product, try to find it
            if not self.current_product and params["product_name"]:
                # This would require an additional API call to find the product by name
                # For simplicity, we'll assume the product is already selected
                raise MCPError("Please select the product first")
            
            return self.update_product_stock(
                self.current_seller["id"],
                self.current_product["id"],
                params["new_stock"]
            )
        
        elif intent == "select_seller":
            if not params.get("seller_id"):
                raise MCPError("Please provide a seller ID")
            return self.get_seller(params["seller_id"])
        
        raise MCPError("I couldn't understand that request. Please try again with more details.")

    def get_conversation_history(self) -> List[Dict]:
        return [entry.to_dict() for entry in self.conversation_history]

    def get_current_context(self) -> Dict:
        return {
            "current_seller": self.current_seller,
            "current_product": self.current_product,
            "context": self.context,
            "pending_operation": self.pending_operation
        }

    def get_health(self):
        try:
            response = requests.get(f"{self.base_url}/actuator/health")
            health_data = self._handle_response(response, "health_check")
            self._add_to_history("health_check", {"status": health_data})
            return health_data
        except MCPError as e:
            logger.error(f"Health check failed: {str(e)}")
            raise

    def create_seller(self, name: str, email: str):
        try:
            data = {
                "name": name,
                "email": email
            }
            response = requests.post(
                f"{self.base_url}/api/sellers",
                headers=self.headers,
                data=json.dumps(data)
            )
            seller_data = self._handle_response(response, "create_seller")
            self.current_seller = seller_data
            self._add_to_history("create_seller", {
                "request": data,
                "response": seller_data
            })
            logger.info(f"Created seller: {name} ({email})")
            return seller_data
        except MCPError as e:
            logger.error(f"Failed to create seller: {str(e)}")
            raise

    def get_seller(self, seller_id: str):
        try:
            response = requests.get(f"{self.base_url}/api/sellers/{seller_id}")
            seller_data = self._handle_response(response, "get_seller")
            self.current_seller = seller_data
            self._add_to_history("get_seller", {
                "seller_id": seller_id,
                "response": seller_data
            })
            logger.info(f"Retrieved seller: {seller_data.get('name')}")
            return seller_data
        except MCPError as e:
            logger.error(f"Failed to get seller: {str(e)}")
            raise

    def add_product(self, seller_id: str, name: str, description: str, price: float, stock: int):
        try:
            data = {
                "name": name,
                "description": description,
                "price": price,
                "stock": stock
            }
            response = requests.post(
                f"{self.base_url}/api/sellers/{seller_id}/products",
                headers=self.headers,
                data=json.dumps(data)
            )
            product_data = self._handle_response(response, "add_product")
            self.current_product = product_data
            self._add_to_history("add_product", {
                "seller_id": seller_id,
                "request": data,
                "response": product_data
            })
            logger.info(f"Added product: {name} (${price})")
            return product_data
        except MCPError as e:
            logger.error(f"Failed to add product: {str(e)}")
            raise

    def update_product_stock(self, seller_id: str, product_id: str, new_stock: int):
        try:
            data = {
                "stock": new_stock
            }
            response = requests.patch(
                f"{self.base_url}/api/sellers/{seller_id}/products/{product_id}/stock",
                headers=self.headers,
                data=json.dumps(data)
            )
            product_data = self._handle_response(response, "update_stock")
            self.current_product = product_data
            self._add_to_history("update_stock", {
                "seller_id": seller_id,
                "product_id": product_id,
                "new_stock": new_stock,
                "response": product_data
            })
            logger.info(f"Updated stock for product {product_id} to {new_stock}")
            return product_data
        except MCPError as e:
            logger.error(f"Failed to update stock: {str(e)}")
            raise

    def get_low_stock_products(self, seller_id: str):
        try:
            response = requests.get(f"{self.base_url}/api/sellers/{seller_id}/products/low-stock")
            products_data = self._handle_response(response, "get_low_stock")
            self._add_to_history("get_low_stock", {
                "seller_id": seller_id,
                "response": products_data
            })
            logger.info(f"Found {len(products_data)} products with low stock")
            return products_data
        except MCPError as e:
            logger.error(f"Failed to get low stock products: {str(e)}")
            raise

def main():
    client = MCPClient()
    
    try:
        # Test health endpoint
        print("Checking system health...")
        health = client.get_health()
        print(f"System status: {health}\n")

        # Create a seller
        print("Creating a new seller account...")
        seller = client.create_seller(
            name="Test Seller",
            email="test@example.com"
        )
        print(f"Successfully created seller account: {seller['name']} (ID: {seller['id']})\n")
        
        seller_id = seller["id"]

        # Get seller details
        print("Retrieving seller profile...")
        seller_details = client.get_seller(seller_id)
        print(f"Found seller profile: {seller_details['name']} ({seller_details['email']})\n")

        # Add a product
        print("Adding a new product to inventory...")
        product = client.add_product(
            seller_id=seller_id,
            name="Test Product",
            description="A test product",
            price=29.99,
            stock=100
        )
        print(f"Successfully added product: {product['name']} (ID: {product['id']})\n")

        # Update stock
        print("Updating product stock...")
        updated_product = client.update_product_stock(
            seller_id=seller_id,
            product_id=product["id"],
            new_stock=95
        )
        print(f"Updated stock for {updated_product['name']}: {updated_product['stock']} units\n")

        # Check low stock products
        print("Checking for low stock products...")
        low_stock = client.get_low_stock_products(seller_id)
        print(f"Found {len(low_stock)} products with low stock\n")

        # Test natural language processing with incomplete information
        print("Testing natural language processing with incomplete information...")
        try:
            # First query with only product name
            result = client.process_natural_language("Add product name is Gaming Mouse")
            print(f"Response: {result['question']}\n")
            
            # Provide price
            result = client.process_natural_language("The price is 49.99")
            print(f"Response: {result['question']}\n")
            
            # Provide stock
            result = client.process_natural_language("The stock is 50")
            print(f"Result: {result}\n")
        except MCPError as e:
            print(f"Natural language error: {str(e)}\n")

        # Display conversation history
        print("\nConversation History:")
        for entry in client.get_conversation_history():
            print(f"[{entry['timestamp']}] {entry['operation']}: {json.dumps(entry['details'], indent=2)}")

    except MCPError as e:
        print(f"Error: {str(e)}")
        if e.status_code:
            print(f"Status code: {e.status_code}")
        if e.details:
            print(f"Details: {json.dumps(e.details, indent=2)}")
    except Exception as e:
        print(f"Unexpected error: {str(e)}")

if __name__ == "__main__":
    main() 