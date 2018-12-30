#!/bin/sh

echo "building"
cd acccounting
npm install -d
npm install express
npm install amqplib
npm install nano --save
echo "running"
npm start
