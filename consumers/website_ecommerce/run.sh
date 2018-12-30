#!/bin/sh

cd product
echo "building"
python3 -m pip install -r requirements.txt --no-cache-dir
echo "running"
python -u app.py