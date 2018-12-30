#!/bin/sh

cd customer
echo "building"
pip install -r requirements.txt
echo "running"
python customer.py