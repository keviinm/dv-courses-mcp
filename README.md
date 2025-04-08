# LLM MCP Client

This project demonstrates how to use a Large Language Model (LLM) as a client for interacting with the MCP (Model-Controller-Product) server.

## Features

- Natural language interface to the MCP API
- Uses OpenAI's GPT models to interpret commands and responses
- Supports all MCP server endpoints through conversational interface
- Intelligent analytical capabilities for data interpretation
- Context-aware responses that adapt to the type of query

## Setup

1. Install the required dependencies:
   ```
   pip install -r requirements.txt
   ```

2. Set up your OpenAI API key as an environment variable:
   ```
   export OPENAI_API_KEY=your_api_key_here
   ```
   
   Alternatively, create a `.env` file with:
   ```
   OPENAI_API_KEY=your_api_key_here
   ```

3. Make sure the MCP server is running on http://localhost:8084 (or update the base_url in the client)

## Usage

Run the LLM MCP client:
```
python llm_mcp_client.py
```

Then enter natural language commands like:
- "Show me all sellers"
- "Create a new seller named Acme Corp with email acme@example.com"
- "Add a product called Widget to seller with ID abc-123"
- "What products does seller abc-123 have?"

### Analytical Queries

The client can also handle analytical queries:
- "How many active sellers do we have?"
- "Which seller has the most products?"
- "What is the average price of all products?"
- "How many products have low stock?"
- "Summarize the performance of all sellers"

## How It Works

1. The LLM interprets your natural language command and determines which API endpoint to call
2. The client makes the appropriate HTTP request to the MCP server
3. The response is sent back to the LLM for interpretation
4. For analytical queries, the LLM analyzes the data and provides a direct answer
5. For regular queries, the LLM provides a natural language explanation of the response

## Examples

Check out the example scripts:
- `example_usage.py`: Basic examples of using the client
- `advanced_example.py`: Examples of analytical queries

## Customization

You can modify the `LLMMCPClient` class to:
- Use a different LLM provider (Anthropic, Google, etc.)
- Add support for more API endpoints
- Customize the system prompt to change the LLM's behavior
- Adjust temperature and other LLM parameters
- Add more analytical capabilities

# MCP Spring Boot Application

This is a Spring Boot application that provides a REST API for managing products and sellers.

## Deploying to AWS Lightsail

### Prerequisites

1. AWS Account with Lightsail access
2. AWS CLI installed and configured
3. Java 17 or later

### Deployment Steps

1. Create a new Lightsail instance:
   ```bash
   aws lightsail create-instances \
     --instance-names mcp-server \
     --availability-zone us-west-2a \
     --blueprint-id amazon_linux_2 \
     --bundle-id nano_2_0
   ```

2. Wait for the instance to be ready and get its public IP:
   ```bash
   aws lightsail get-instance --instance-name mcp-server
   ```

3. Copy files to the instance:
   ```bash
   scp -i LightsailDefaultKey-us-west-2.pem \
     target/courses-0.0.1-SNAPSHOT.jar \
     setup-lightsail.sh \
     mcp-service.service \
     ubuntu@<INSTANCE_IP>:/home/ubuntu/
   ```

4. SSH into the instance:
   ```bash
   ssh -i LightsailDefaultKey-us-west-2.pem ubuntu@<INSTANCE_IP>
   ```

5. Run the setup script:
   ```bash
   ./setup-lightsail.sh
   ```

6. Check the service status:
   ```bash
   sudo systemctl status mcp-service
   ```

### Environment Variables

The application requires the following environment variables:

- `AWS_ACCESS_KEY_ID`: Your AWS access key
- `AWS_SECRET_KEY`: Your AWS secret key
- `AWS_REGION`: AWS region (default: us-west-2)
- `AWS_DYNAMODB_ENDPOINT`: DynamoDB endpoint (default: http://localhost:8000)

These are configured in the systemd service file.

### API Endpoints

The application runs on port 8084 and provides the following endpoints:

- `/api/sellers`: Manage sellers
- `/api/products`: Manage products

### Monitoring

To view application logs:
```bash
sudo journalctl -u mcp-service -f
```