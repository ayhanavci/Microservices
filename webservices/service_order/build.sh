#!/bin/sh

echo "Creating path" 
mkdir -p /order
cd order
echo "building"
mvn package
echo "running"
mvn exec:java