## Introduction

This is the result of my study on Microservices. It is an online shopping system and the details are here: [https://lain.run/projects/Microservices/](https://lain.run/projects/Microservices/). 

## Implemented Concepts

* Microservices pattern
* CQRS
* Event Bus
* Event Store
* Database per Service
* Event Broker Pattern
* Saga - Choreography
* Reverse Proxy
* RESTful services

## User Stories

Template: As a __[ USER ]__ I want to __[ ACTION ]__ So that __[ REASONING ]__

User Personas:

1. Customer
2. Product Manager

Features:

1. View Products
2. Shop
3. View Order Status
4. View User Profile
5. Get Invoice
6. Modify products, prices, stocks
7. Modify user credits

| User Story | Acceptance Criteria  |
| ---------- | -------------------- |
|As a customer, I want to browse for products so that I can place an order.| View product categories<br>View Products<br>Ability to order each product<br>Get an Invoice|
|As a customer, I want to view my past orders so that it guides my future purposes. |View orders. Display order status, product info, order date|
|As a customer, I want to view my profile information so that I can make adjustments. |View user information and credit<br>Edit name, password, email|
|As a product manager, I want to add/edit/delete products so that customers can buy them|View/Add/Edit/Delete products. Names, prices, suppliers, units in stock |
|As a product manager, I want to view all customer order states so that I can assist if needed|View all orders and their states history in the system|
|As a product manager, I want to edit customer credit so that they can purchase products|View all users and their credit<br>Edit credits|


## Modules

I used Python with Flask often because it is arguably one of the fastest and cleanest ways to prototype anything. I would implement everything in Python if I didn't want to introduce some variety. For Event Store, I used two docker containers. SDK to build for Linux, Runtime for deployment. 

| Module | Category  | Programming Language | Sdk | Docker |
| ------ | --------- | -------------------- | -------- | ------ |
|ECommerce Website|Consumer|Python|Flask|[python:alpine](https://hub.docker.com/_/python/)|
|Manager Website|Consumer|Python|Flask|[python:alpine](https://hub.docker.com/_/python/)|
|Native Android Application|Consumer|Java|Android SDK|-|
|Accounting Database|Database|-|-|[couchdb:latest](https://hub.docker.com/_/couchdb)|
|Customer Database|Database|-|-|[redis:alpine](https://hub.docker.com/_/redis)|
|Event Store Database|Database|-|-|[mongo](https://hub.docker.com/_/mongo)<br>[mongo-express](https://hub.docker.com/_/mongo-express)|
|Order Database|Database|-|-|[postgres:alpine](https://hub.docker.com/_/postgres)|
|Event Bus|Support Tool|-|-|[rabbitmq:management](https://hub.docker.com/_/rabbitmq)|
|Event Store|Support Tool|C#|.NET Core 2.1|[dotnet:2.1-sdk-alpine](https://hub.docker.com/r/microsoft/dotnet) <br> [dotnet:2.1-aspnetcore-runtime-alpine](https://hub.docker.com/r/microsoft/dotnet)|
|Reverse Proxy|Support Tool|-|-|[nginx:alpine](https://hub.docker.com/_/nginx)|
|Accounting Web Service|Web Service|Javascript|Node|[node:alpine](https://hub.docker.com/_/node)|
|Customer Web Service|Web Service|Python|Flask|[python:alpine](https://hub.docker.com/_/python/)|
|Order Web Service|Web Service|Java|JDK-Jersey|[maven:3.6-jdk-8-alpine](https://hub.docker.com/_/maven)|
|Product Web Service|Web Service|Python|Flask|[python:alpine](https://hub.docker.com/_/python/)|


Api by service;

| Customer (Python) | Order Service(Java)  | Product Service(Python)  | Accounting Service(JS) |
| ---------------- | -------------- | ---------------- | ------------------ |
| login-user | place-order | get-products | get-revenue |
| add-user | get-orders | get-all-products ||
| update-user || get-product-details ||
| get-user || add-new-product ||
| get-all-users || update-product ||
| get-credit || delete-product ||
| set-credit || get-categories ||
||| add-new-category ||
||| update-category ||
||| delete-category ||

## Communication Topography

As follows;

![allsystem](https://github.com/ayhanavci/Microservices/blob/master/img/allsystem.png)



## Order Saga

The only Saga sample. Occurs when user places an order request for a product. Not all rollbacks are implemented. It looks messy but what happens here is: Customer places an order, If the customer has enough credit for the product price and if the product is in stock, the order succeeds.

![saga](https://github.com/ayhanavci/Microservices/blob/master/img/ordersaga.png)

## Running the Project

All modules are prepared for Docker Compose. You need to create Docker networks written inside docker compose files, then run the compose files. The classic option is running compose from terminal. You need to repeat the following process for each module.

1. Open a terminal. Change directory to the module
2. Type ```docker-compose up```


That's it. Everything should install inside its own container and run in there. Aside from Docker itself, you don't need to explicitly install anything else.

Only couchDB requires the following command after it starts:

```curl -X PUT http://accounting_usr:accounting_pass@127.0.0.1:5984/_users```

Both websites run on ports 5001 and 5002 both of which you can edit from their yml files. On Android project, you need to open the settings (upper right corner) inside the app and change the IP / Host of the reverse proxy server.

## Licence

MIT

## Author

Ayhan AVCI 2018 

ayhanavci@gmail.com 

[lain.run](https://lain.run)