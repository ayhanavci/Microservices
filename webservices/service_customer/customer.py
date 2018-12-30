'''
Customer Service. Provides Customer API as described on def index()

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/

'''
from flask import Flask
from flask import request
from flask import json
from flask import jsonify
import redis
import requests
import os
import pika
import threading
import datetime
import uuid
import asyncio

app = Flask(__name__)
redis_db = redis.StrictRedis(host=os.environ["DATABASE_IP"], 
                             password=os.environ["DATABASE_PASSWORD"], 
                             port=os.environ["DATABASE_PORT"], 
                             db=0)
   
@app.route('/', methods=['GET'])
def index():
  services = {    
    "login-user": {    
      "UserName": "String", 
      "Password": "String"   
    },
    "add-user": {     
      "UserName": "String", 
      "Password": "String", 
      "FullName": "String",
      "Email": "String", 
      "Credit": "String"
    },
    "update-user": {    
      "UserName": "String", 
      "Password": "String", 
      "FullName": "String",
      "Email": "String", 
      "Credit": "String" 
    },
    "get-user": { 
      "UserName": "String"    
    },
    "get-all-users": { 
      
    },
    "get-credit": {     
      "UserName": "String"
    },   
    "set-credit": {     
      "UserName": "String",
      "Credit": "String"
    }   
  }
  return jsonify(services=services), 200

@app.route('/login-user/', methods=['POST'])
def login_user():    
  user_name = request.json['UserName'] 
  password = request.json['Password']
  result = { "Status": "Success"}
  if not redis_db.exists(user_name):
    result = { "Status": "User Not Found"}    
  else:
    print(redis_db.get(user_name))
    user_data = json.loads(redis_db.get(user_name))
    if user_data['Password']  != password:      
      result = { "Status": "Wrong password"}  
  return jsonify(result=result), 200

@app.route('/add-user/', methods=['POST'])
def add_user():  
  user_name = request.json['UserName']  
  if redis_db.exists(user_name):
    result = { "Status": "User already exists"} 
  else:    
    store_data = {}
    store_data['Action'] = "Add New Customer"    
    store_data['UserName'] = request.json['UserName']  
    store_data['FullName'] = request.json['FullName']  
    store_data['Password'] = request.json['Password']  
    store_data['Email'] = request.json['Email']      
    store_data['Credit'] = request.json['Credit']      
    send_event_store_data("add_customer", store_data)
    result = { "Status": "Success"} 
  return jsonify(result=result), 200     

@app.route('/update-user/', methods=['POST'])
def update_user():  
  user_name = request.json['UserName']  
  if not redis_db.exists(user_name):
    result = { "Status": "User not found"} 
  else:    
    user_data = json.loads(redis_db.get(user_name))
    store_data = {}
    store_data['Action'] = "Update Customer"    
    store_data['UserName'] = request.json['UserName']  
    store_data['FullName'] = request.json['FullName']
    store_data['Password'] = request.json['Password']  
    store_data['Email'] = request.json['Email']      
    store_data['Credit'] =  user_data['Credit'] #Preserve credit
    send_event_store_data("update_customer", store_data)    
    print("update_user:{0}".format(store_data['Action']))          
    result = { "Status": "Success"} 
  return jsonify(result=result), 200

@app.route('/set-credit/', methods=['POST'])
def set_credit():  
  user_name = request.json['UserName'] 
  result = { "Status": "Success"}
  if not redis_db.exists(user_name):
    result = { "Status": "User not found"}    
  else:
    user_data = json.loads(redis_db.get(user_name))
    store_data = {}
    store_data['Action'] = "Set Credit"    
    store_data['UserName'] = user_data['UserName']  
    store_data['FullName'] = user_data['FullName']  
    store_data['Password'] = user_data['Password']  
    store_data['Email'] = user_data['Email']      
    store_data['Credit'] = request.json['Credit'] #Only update credit
    send_event_store_data("set_credit", store_data)     
  return jsonify(result="result"), 200

@app.route('/get-credit/', methods=['GET'])
def get_credit():  
  user_name = request.json['UserName'] 
  result = { "Status": "Error"}  
  if not redis_db.exists(user_name):
    result = { "Status": "User not found"}    
  else:
    user_data = json.loads(redis_db.get(user_name))
    app.logger.info('get-credit: %s', user_data)        
    result = { "Status": "Success", "Credit": user_data['Credit']}
  return jsonify(result=result), 200

@app.route('/get-user/<user_name>', methods=['GET'])
def get_user(user_name):
  if not redis_db.exists(user_name):
    result = { "Status": "User not found"} 
  else:
    user_data = json.loads(redis_db.get(user_name))
    result = { "Status": "Success", "User Info": user_data }     
  return jsonify(result=result), 200
 
@app.route('/get-all-users/', methods=['GET'])
def get_all_users():
  if redis_db.keys().__len__() == 0:
    result = { "Status": "No User Found"} 
  else:
    user_data = []
    for user_name in redis_db.keys():
      user_data.append(json.loads(redis_db.get(user_name)))
    result = { "Status": "Success", "Users": user_data }     
  return jsonify(result=result), 200

def event_add_new_customer(user_data):
  event_set_user_data(user_data)
  print("event_add_new_customer - {0}".format(user_data))

def event_update_customer(user_data):
  event_set_user_data(user_data)
  print("event_update_customer - {0}".format(user_data))      

def event_set_credit(user_data):
  event_set_user_data(user_data)
  print("event_set_credit - {0}".format(user_data))    

def event_new_order_placed(message):  
  print(message) 
  message_json = json.loads(message)
  data_json = json.loads(message_json['Data'])  
  user_data = json.loads(redis_db.get(data_json['CustomerId']))
  app.logger.info('event_new_order_placed: %s', user_data)        
  result = { "OrderID": data_json['OrderID'], "Credit":user_data['Credit'] }
  send_order_saga_data("CreditInfo", json.dumps(result))

def event_order_finalized(message):
  print("event_order_finalized:" + message) 
  message_json = json.loads(message)
  data_json = json.loads(message_json['Data']) 
  user_data = json.loads(redis_db.get(data_json['CustomerId']))
  print("event_order_finalized: Customer:" + data_json['CustomerId']) 
  credit = user_data['Credit']
  price = data_json['Price']
  final_credit = 0 if float(credit) - float(price) < 0 else float(credit) - float(price)
  user_data['Credit'] = final_credit
  redis_db.set(data_json['CustomerId'], json.dumps(user_data))
  redis_db.save()  

def event_set_user_data(user_data):
  local_data = {}    
  local_data['UserName'] = user_data['UserName']  
  local_data['FullName'] = user_data['FullName']  
  local_data['Password'] = user_data['Password']  
  local_data['Email'] = user_data['Email']      
  local_data['Credit'] = user_data['Credit']      
  redis_db.set(local_data['UserName'], json.dumps(local_data))
  redis_db.save()

bus_user_name = os.environ["EVENT_BUS_USERNAME"]
bus_password = os.environ["EVENT_BUS_PASSWORD"]
bus_hostname = os.environ["EVENT_BUS_IP"]

credentials = pika.PlainCredentials(username=bus_user_name, password=bus_password)
parameters = pika.ConnectionParameters(bus_hostname, 5672, '/', credentials)
connection = pika.BlockingConnection(parameters)

#Event Bus Addressing
send_queue_name = "msdemo_queue_event_store"
send_exchange_name = "msdemo_exchange_event_store"
send_routing_key = "msdemo_routingkey_event_store"

receive_queue_name = "msdemo_queue_customer"
receive_exchange_name = "msdemo_exchange_customer"
receive_routing_key = "msdemo_routingkey_customer"

exchange_name_order = "msdemo_exchange_order"

saga_exchange_name_order = "msdemo_exchange_saga_order"
saga_routing_key_order = "msdemo_routingkey_saga_order"
saga_routing_key_orderresponse = "msdemo_routingkey_saga_orderresponse"

def listener_callback(ch, method, properties, body):  
  response_json = json.loads(body)
  print("Listener Callback Key:{0} Json:{1}".format(method.routing_key, response_json))
  if (method.routing_key == receive_routing_key):
    if (response_json['Data']['Action'] == "Add New Customer"):
      event_add_new_customer(response_json['Data'])  
    elif (response_json['Data']['Action'] == "Update Customer"):
      event_update_customer(response_json['Data'])  
    elif (response_json['Data']['Action'] == "Set Credit"):
      event_set_credit(response_json['Data'])    
  elif (method.routing_key == saga_routing_key_order):
    print("Processing Order Placed. Event:{0}".format(response_json['Event']))
    if (response_json['Event'] == "OrderPlaced"):
      event_new_order_placed(response_json['Data'])
    elif (response_json['Event'] == "OrderFinalized"):
      event_order_finalized(response_json['Data'])
  
def init_event_bus():
  threading.Thread(target=start_listener).start()  
  start_sender()

def start_listener():
  #Receive from Event Store  
  receive_channel = connection.channel()
  receive_channel.exchange_declare(exchange=receive_exchange_name, exchange_type='direct')
  receive_channel.exchange_declare(exchange=saga_exchange_name_order, exchange_type='direct')
  receive_channel.queue_declare(
                queue=receive_queue_name, 
                durable=True,
                exclusive=False,
                auto_delete=False,
                arguments=None)

  receive_channel.queue_bind(exchange=receive_exchange_name,
                    queue=receive_queue_name,
                    routing_key=receive_routing_key)
  
  receive_channel.queue_bind(exchange=saga_exchange_name_order,
                    queue=receive_queue_name,
                    routing_key=saga_routing_key_order)

  receive_channel.basic_qos(prefetch_size=0, prefetch_count=1)
  receive_channel.basic_consume(listener_callback, queue=receive_queue_name, no_ack=True)
  receive_channel.start_consuming()          
 
send_channel = connection.channel()
def start_sender():
  #Send to Event Store      
  send_channel.exchange_declare(exchange=send_exchange_name, exchange_type='direct')
  send_channel.exchange_declare(exchange=send_exchange_name, exchange_type='direct')
  send_channel.queue_declare(queue=send_queue_name, durable=True, exclusive=False, auto_delete=False, arguments=None)
  send_channel.queue_bind(queue=send_queue_name, exchange=send_exchange_name, routing_key=send_routing_key)  

def send_event_store_data(event_type, data): 
  item = {}
  item_data = {}
  item["Aggregate"] = "Customer"
  item["Topic"] = "msdemo_topic.customer.{0}".format(event_type)
  item["Timestamp"] = datetime.datetime.now().strftime("%d/%m/%Y %H:%M:%S:%f")  
  item["Version"] = "1.0"
  item["BUS_ExchangeName"] = receive_exchange_name
  item["BUS_Queue"] = receive_queue_name
  item["BUS_RoutingKey"] = receive_routing_key
  item["Data"] = data
  message = json.dumps(item)
  print(message)
  try:
    send_channel.basic_publish(exchange=send_exchange_name,
                        routing_key=send_routing_key,
                        body=message,
                        properties=pika.BasicProperties(
                          delivery_mode = 2, # make message persistent
                        ))
  except pika.exceptions.ConnectionClosed:
    print("send_event_store_data Exception. Connection closed")
  except:
    print("send_event_store_data Exception")

def send_order_saga_data(event_type, data): 
  print("send_order_saga_data type:{0} data:{1}".format(event_type, data))
  item = {}
  item_data = {}
  item["Event"] = event_type
  item["Data"] = data
  message = json.dumps(item)
  print(message)
  try:
    send_channel.basic_publish(exchange=exchange_name_order,
                        routing_key=saga_routing_key_orderresponse,
                        body=message,
                        properties=pika.BasicProperties(
                          delivery_mode = 2, # make message persistent
                        ))
  except pika.exceptions.ConnectionClosed:
    print("send_order_saga_data Exception. Connection closed")
  except:
    print("send_order_saga_data Exception")
  
if __name__ == "__main__": 
  init_event_bus()
  app.run(port=80, host="0.0.0.0", debug=True)