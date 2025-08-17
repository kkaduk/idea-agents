#!/bin/bash

echo "Building Banking Product Development Microservices..."

# Build parent and shared config first
mvn clean install -pl shared-config -am

# Build all agent microservices
mvn clean package -pl idea-creator-agent
mvn clean package -pl risk-estimator-agent  
mvn clean package -pl idea-critic-agent
mvn clean package -pl idea-finalizer-agent
mvn clean package -pl human-agent
mvn clean package -pl orchestration-service

echo "All microservices built successfully!"
