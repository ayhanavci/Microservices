#!/bin/sh

echo "restoring" 
cd app
dotnet restore
echo "building"
dotnet publish -c Release -o ./event_store_deployment/
