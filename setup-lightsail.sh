#!/bin/bash

# Update package list
sudo apt-get update

# Install Java 17 and Maven
sudo apt install -y openjdk-17-jdk maven

# Create application directory
sudo mkdir -p /home/ubuntu/app

# Copy the JAR file to the application directory
sudo cp mcp-0.0.1-SNAPSHOT.jar /home/ubuntu/app/

# Copy service file
sudo cp mcp-service.service /etc/systemd/system/

# Set proper permissions
sudo chown -R ubuntu:ubuntu /home/ubuntu/app

# Reload systemd
sudo systemctl daemon-reload

# Enable the service
sudo systemctl enable mcp-service

# Start the service
sudo systemctl restart mcp-service 