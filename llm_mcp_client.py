import requests
import json
import os
from openai import OpenAI

class LLMMCPClient:
    def __init__(self, base_url="http://localhost:8084", model="gpt-3.5-turbo"):
        self.base_url = base_url
        self.headers = {
            "Content-Type": "application/json"
        }
        self.client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
        self.model = model
        self.conversation_history = []

    def _add_to_history(self, role, content):
        self.conversation_history.append({"role": role, "content": content})

    def _get_llm_response(self, prompt, is_analytical=False):
        system_content = (
            "You are an AI assistant that interacts with a product management API. "
            "You can create sellers, add products, and manage inventory. "
            "When interpreting commands, be precise about which API endpoint to call. "
            "When analyzing data, provide concise, direct answers to questions."
        )
        
        if is_analytical:
            system_content += (
                " For analytical queries, analyze the data and provide a direct answer. "
                "For example, if asked 'how many active sellers are there?', count the active sellers "
                "and respond with just the number, not the full list."
            )
        
        messages = [
            {"role": "system", "content": system_content},
            *self.conversation_history,
            {"role": "user", "content": prompt}
        ]
        
        response = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            temperature=0.7,
            max_tokens=500
        )
        
        response_content = response.choices[0].message.content
        self._add_to_history("assistant", response_content)
        return response_content

    def execute_command(self, command):
        """Execute a natural language command against the MCP server"""
        # Check if this is an analytical query
        is_analytical = any(keyword in command.lower() for keyword in 
                           ["how many", "count", "total", "sum", "average", "most", "least"])
        
        # First, get the LLM to interpret the command
        interpretation = self._get_llm_response(
            f"Interpret this command and determine what API call to make: {command}\n"
            f"Available endpoints:\n"
            f"- GET /api/sellers - List all sellers\n"
            f"- POST /api/sellers - Create a new seller (requires name, email)\n"
            f"- GET /api/sellers/{'{id}'} - Get seller details\n"
            f"- POST /api/sellers/{'{id}'}/products - Add a product to a seller (requires name, description, price, stock)\n"
            f"Return a JSON object with 'method', 'endpoint', and 'data' fields."
        )
        
        try:
            # Parse the LLM's response to get the API call details
            api_call = json.loads(interpretation)
            method = api_call.get("method", "GET")
            endpoint = api_call.get("endpoint", "/api/sellers")
            data = api_call.get("data", {})
            
            # Make the API call
            if method.upper() == "GET":
                response = requests.get(f"{self.base_url}{endpoint}")
            elif method.upper() == "POST":
                response = requests.post(
                    f"{self.base_url}{endpoint}",
                    headers=self.headers,
                    data=json.dumps(data)
                )
            else:
                return f"Unsupported method: {method}"
            
            # Get the LLM to interpret the response
            response_data = response.json()
            
            # For analytical queries, provide specific instructions to the LLM
            if is_analytical:
                interpretation = self._get_llm_response(
                    f"The API call returned this response: {json.dumps(response_data)}\n"
                    f"Original question: {command}\n"
                    f"Please analyze this data and provide a direct answer to the question. "
                    f"For example, if asked 'how many active sellers are there?', respond with just the number, "
                    f"not the full list of sellers."
                )
            else:
                interpretation = self._get_llm_response(
                    f"The API call returned this response: {json.dumps(response_data)}\n"
                    f"Please explain this response in natural language."
                )
            
            return interpretation
            
        except Exception as e:
            return f"Error executing command: {str(e)}"

def main():
    client = LLMMCPClient()
    
    print("LLM MCP Client")
    print("Type 'exit' to quit")
    
    while True:
        command = input("\nEnter a command: ")
        if command.lower() == "exit":
            break
            
        result = client.execute_command(command)
        print(f"\nResult: {result}")

if __name__ == "__main__":
    main() 