/*

Communicates with the Event Bus. Sends and Receives Order & Order Saga events

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/

*/
package com.ms_demo;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.util.Calendar;
import java.text.SimpleDateFormat;

public class EventBus {
  //Event Store
  private String event_store_queue_name = "msdemo_queue_event_store";
  private String event_store_exchange_name = "msdemo_exchange_event_store";  
  private String event_store_routing_key = "msdemo_routingkey_event_store"; 
      
  //Order Events Listener
  private String receive_exchange_name = "msdemo_exchange_order"; //Used by Event Store & Order Saga
  private String receive_queue_name = "msdemo_queue_order";       //Used by Event Store
  private String receive_routing_key_recorded = "msdemo_routingkey_orderrecorded"; //Used by Event Store  
  private String saga_routing_key_order = "msdemo_routingkey_saga_order"; //Used by this service
  private String saga_routing_key_orderresponse = "msdemo_routingkey_saga_orderresponse"; //Used by Order Saga
  private String saga_exchange_name_order = "msdemo_exchange_saga_order";

  //Info Broadcasts
  private String exchange_name_info = "msdemo_exchange_info";
  Map<String, String> map_topics = new HashMap<String, String>();

  //Event Bus
  private String host = System.getenv("EVENT_BUS_IP");
  private String user_name = System.getenv("EVENT_BUS_USERNAME");
  private String password = System.getenv("EVENT_BUS_PASSWORD");

  private DatabaseOperations database;
  
  private ConnectionFactory factory;
  private Connection connection;
  private Channel sender_channel;
  private Channel topic_listen_channel;
  private Channel order_listen_channel;

  EventBus(DatabaseOperations db)
  {
    //Topics to register.      
    map_topics.put("order", "msdemo.topic.order.*");
    map_topics.put("system", "msdemo.topic.system.*");

    //Fired by this service
    map_topics.put("order.placed", "msdemo.topic.order.placed");
    map_topics.put("order.confirmed", "msdemo.topic.order.approved");
    map_topics.put("order.rejected", "msdemo.topic.order.rejected");
    
    //Fired by Event Store
    map_topics.put("order.placed.recorded", "msdemo.topic.order.placed.recorded"); //Upon order.placed
    map_topics.put("order.approved.recorded", "msdemo.topic.order.approved.recorded"); //Upon order.approved
    map_topics.put("order.rejected.recorded", "msdemo.topic.order.rejected.recorded"); //Upon order.rejected

    //Fired by Products aggregate
    map_topics.put("order.stock.confirmed", "msdemo.topic.order.stock.confirmed"); //Upon order.placed.recorded, after checking stocks. +Returns price per unit.
    map_topics.put("order.stock.rejected", "msdemo.topic.order.stock.rejected"); //Upon order.placed.recorded, after checking stocks. +Returns price per unit.
    
    //Fired by Customer aggregate
    map_topics.put("order.credit.confirmed", "msdemo.topic.order.credit.confirmed"); //Upon order.placed.recorded, after checking customer credit.
    map_topics.put("order.credit.rejected", "msdemo.topic.order.credit.rejected"); //Upon order.placed.recorded, after checking customer credit.

    database = db;         
  }

  public boolean connect() 
  {
    factory = new ConnectionFactory();
    factory.setUsername(user_name);
    factory.setPassword(password);
    factory.setHost(host);
    try {
      System.out.println(" Connecting to event bus with host:[" + host + "]user:[" + user_name + "] password:[" + password + "]");
      connection = factory.newConnection();
    } catch (Exception e) {
      System.out.println(" bus connect Exception:" + e.getMessage());
      return false;
    }
   
    return true;
  }
  public boolean run() 
  {        
    if (!connect()) {
      System.out.println(" EventBus cannot connect");
      return false;
    }
    init_sender();
    new Thread(() -> listen_topics()).start();    
    new Thread(() -> init_order_queue_listener()).start();   
    return true;
  } 
  
  public void process_order_event(String message)
  {    
    String str_action = "", str_result = "", str_data = "";
    JsonObject message_json;
    
    //Get Message as JSon
    try {
      message_json = new JsonParser().parse(message).getAsJsonObject();
      
    } catch (Exception ex) {
      System.out.println("Order Event, Exception: " + ex.getMessage());
      return;
    }    

    //Get Result from Message
    JsonElement result_element = message_json.get("Result");
    if (result_element == null) {
      System.out.println("Result is empty");
      return;
    }
    str_result = result_element.getAsString();

    //Get Data from Message
    JsonElement data_element = message_json.get("Data"); //Data Element
    if (data_element == null) {
      System.out.println("Data is empty");
      return;
    }

    str_data = data_element.getAsString(); //Data String
    if (str_data.isEmpty()) {
      System.out.println("Data string is empty");
      return;
    }    

    JsonObject data_json;
    try {
      data_json = new JsonParser().parse(str_data).getAsJsonObject(); //Data Json
      if (data_json == null) {
        System.out.println("data_json empty");
        return;
      }
      
    } catch (Exception ex) {
      System.out.println("Order Event, Exception: " + ex.getMessage());
      return;
    }     
        
    if (data_json.get("Action") != null)
      str_action = data_json.get("Action").getAsString();    

    if (str_action.equals("Place Order") && str_result.equals("Success")) {      
      System.out.println("Processing Place Order event with Success result");
      String str_productid = "", str_orderid = "", str_customerid = "", str_timestamp = "", str_price = "", str_product_name = "";
      if (data_json.get("OrderID") != null)
        str_orderid = data_json.get("OrderID").getAsString(); 
      if (data_json.get("ProductId") != null)
        str_productid = data_json.get("ProductId").getAsString(); 
      if (data_json.get("CustomerId") != null)
        str_customerid = data_json.get("CustomerId").getAsString(); 
      if (data_json.get("TimeStamp") != null)
        str_timestamp = data_json.get("TimeStamp").getAsString(); 
      if (data_json.get("Price") != null)
        str_price = data_json.get("Price").getAsString(); 
      if (data_json.get("ProductName") != null)
        str_product_name = data_json.get("ProductName").getAsString(); 
      
      if (str_productid.isEmpty() || str_orderid.isEmpty() || str_customerid.isEmpty() || str_timestamp.isEmpty() || str_price.isEmpty()) {
        System.out.println("Empty Fields");
        return;
      }      
      Boolean insert_result = database.InsertOrder(str_orderid, str_productid, str_customerid, str_price, str_timestamp, str_product_name);
      if (insert_result == false) {
        System.out.println("Insert failed. Stopping opeartions");
        //Nothing updated yet. Send cancel order event to event store and stop here.
        //TODO
        return;
      }
      System.out.println("Order Inserted.");
      SendDataToOrderSaga("OrderPlaced", message);
      
    }
    else if (str_action.equals("Order Approved") && str_result.equals("Success")) {   
      System.out.println("Processing Order Approved event with Success result");
      String str_orderid = "";
      if (data_json.get("OrderId") != null)
        str_orderid = data_json.get("OrderId").getAsString();       
      if (str_orderid.isEmpty()) {
        System.out.println("Empty Fields");
        return;
      }      
      database.UpdateOrderStatus(str_orderid, DatabaseOperations.ORDER_STATUS_FINALIZED);      
      System.out.println("Order Updated.");
      SendDataToOrderSaga("OrderFinalized", message);
    }
  }
  public void process_orderresponse_event(String message) 
  {
    System.out.println("process_orderresponse_event: " + message);
    JsonObject message_json;
    
    //Get Message as JSon
    try {
      message_json = new JsonParser().parse(message).getAsJsonObject();
      
    } catch (Exception ex) {
      System.out.println("Order Response Event, Exception: " + ex.getMessage());
      return;
    }    
    
    //Get Event from Message
    JsonElement event_element = message_json.get("Event");
    if (event_element == null) {
      System.out.println("Event is empty");
      return;
    }
    String str_event = event_element.getAsString();
    
    //Get Data from Message
    JsonElement data_element = message_json.get("Data"); //Data Element
    if (data_element == null) {
      System.out.println("Data is empty");
      return;
    }
    String str_data = data_element.getAsString(); //Data String
    if (str_data.isEmpty()) {
      System.out.println("Data string is empty");
      return;
    }  
    
    JsonObject data_json;
    try {
      data_json = new JsonParser().parse(str_data).getAsJsonObject(); //Data Json
      if (data_json == null) {
        System.out.println("data_json empty");
        return;
      }
      
    } catch (Exception ex) {
      System.out.println("Order Response Event, Exception: " + ex.getMessage());
      return;
    }     
    
    if (str_event.equals("CreditInfo")) {      
      System.out.println("CreditInfo received for Order:" + data_json.get("OrderID").getAsString() + 
                                                " Value: " + data_json.get("Credit").getAsString());
      
      database.UpdateCredit(data_json.get("OrderID").getAsString(), data_json.get("Credit").getAsFloat(), DatabaseOperations.CREDIT_STATUS_RECEIVED);                             
      ProcessOrderStateChanged(data_json.get("OrderID").getAsString());
    }
    else if (str_event.equals("PriceAndStock")) {      
      System.out.println("PriceAndStock received for Order:" + data_json.get("OrderID").getAsString() + 
                                                " Price: " + data_json.get("Price").getAsString() + 
                                                " Stock: " + data_json.get("Stock").getAsString());
      database.UpdatePriceAndStock(data_json.get("OrderID").getAsString(), 
                                   data_json.get("Stock").getAsInt(), 
                                   data_json.get("Price").getAsFloat(), 
                                   DatabaseOperations.PRICEANDSTOCK_STATUS_RECEIVED);
      ProcessOrderStateChanged(data_json.get("OrderID").getAsString());                                   
      
    }
  }
  private void ProcessOrderStateChanged(String order_id) 
  {
    System.out.println("ProcessOrderStateChanged for " + order_id);
    JsonObject order_json = database.GetOrders("Single", order_id);
    String query_result = order_json.get("result").toString().replaceAll("\"", "");
    System.out.println("ProcessOrderStateChanged query_result " + query_result + " REsult:" + query_result.equals("Success"));
    if (query_result.equals("Success")) {
      JsonArray rows = order_json.get("Data").getAsJsonArray();
      JsonObject order_row = rows.get(0).getAsJsonObject();

      String product_id = order_row.get("PRODUCT").getAsString();
      String customer_id = order_row.get("USERID").getAsString();
      String time_stamp = order_row.get("TIME").getAsString();
      Float price = order_row.get("PRICE").getAsFloat();
      Float credit = order_row.get("CREDIT").getAsFloat();
      int order_status = order_row.get("ORDERSTATUS").getAsInt();
      int stock_status = order_row.get("STOCKSTATUS").getAsInt();
      int credit_status = order_row.get("CREDITSTATUS").getAsInt();
      int stock = order_row.get("STOCK").getAsInt();
      

      if (order_status != DatabaseOperations.ORDER_STATUS_PENDING) {
        System.out.println("ProcessOrderStateChanged for " + order_id + " order has already been processed");
        return;
      }      

      Boolean stock_approved = false;
      Boolean credit_approved = false;

      if (stock_status == DatabaseOperations.PRICEANDSTOCK_STATUS_RECEIVED && credit_status == DatabaseOperations.CREDIT_STATUS_RECEIVED) {
        if (credit >= price) {
          System.out.println("ProcessOrderStateChanged for " + order_id + " Credit: " + credit +  " >= Price:" + price);
          credit_approved = true;
          database.UpdateCredit(order_id, credit, DatabaseOperations.CREDIT_STATUS_APPROVED);
        }
        else {
          System.out.println("ProcessOrderStateChanged for " + order_id + " Credit: " + credit +  " < Price:" + price);
          database.UpdateCredit(order_id, credit, DatabaseOperations.CREDIT_STATUS_DENIED);          
        }
        if (stock > 0) {
          System.out.println("ProcessOrderStateChanged for " + order_id + " unit found in stocks: " + stock);
          stock_approved = true;
          database.UpdatePriceAndStock(order_id, stock, price, DatabaseOperations.PRICEANDSTOCK_STATUS_APPROVED);
        }
        else {
          System.out.println("ProcessOrderStateChanged for " + order_id + " unit not found in stocks: " + stock);
          database.UpdatePriceAndStock(order_id, stock, price, DatabaseOperations.PRICEANDSTOCK_STATUS_DENIED);          
        }
        if (stock_approved && credit_approved) {
          System.out.println("ProcessOrderStateChanged for " + order_id + " success!");
          database.UpdateOrderStatus(order_id, DatabaseOperations.ORDER_STATUS_APPROVED);
          JsonObject event_bus_json = new JsonObject();
          event_bus_json.addProperty("Action", "Order Approved");         
          event_bus_json.addProperty("OrderId", order_id);
          event_bus_json.addProperty("ProductId", product_id);
          event_bus_json.addProperty("CustomerId", customer_id);
          event_bus_json.addProperty("Price", price);
          event_bus_json.addProperty("Amount", 1);
          event_bus_json.addProperty("Time", time_stamp);

          SendDataToEventStore("order_approved", event_bus_json.toString());
        }
        else {
          String reason = "";
          if (!stock_approved) 
            reason = "[Out of Stock] ";          
          if (!credit_approved)
            reason += " [Insufficient Customer Credits]";        
          
          database.UpdateOrderStatus(order_id, DatabaseOperations.ORDER_STATUS_CANCELED);
          JsonObject event_bus_json = new JsonObject();
          event_bus_json.addProperty("Action", "Order Denied");         
          event_bus_json.addProperty("OrderId", order_id);
          event_bus_json.addProperty("ProductId", product_id);
          event_bus_json.addProperty("CustomerId", customer_id);
          event_bus_json.addProperty("Price", price);
          event_bus_json.addProperty("Amount", 1);
          event_bus_json.addProperty("Time", time_stamp);
          event_bus_json.addProperty("Reason", reason);
          
          SendDataToEventStore("order_denied", event_bus_json.toString());        
        }
        
      }     
    }
    else 
      System.out.println("ProcessOrderStateChanged for " + order_id + " failed. " + query_result);
  }
  public void init_sender() 
  {    
    try {
      //For event store replies
      sender_channel = connection.createChannel();
      sender_channel.queueDeclare(event_store_queue_name, true, false, false, null);
      sender_channel.exchangeDeclare(event_store_exchange_name, "direct");          
      sender_channel.exchangeDeclare(saga_exchange_name_order, "direct");                
      sender_channel.queueBind(event_store_queue_name, event_store_exchange_name, event_store_routing_key);      
      
    } catch (Exception e) {
      System.out.println(" init_sender Exception:" + e.getMessage());
    }
  }
  public void init_order_queue_listener()
  {
    try {
      order_listen_channel = connection.createChannel();
      order_listen_channel.queueDeclare(receive_queue_name, true, false, false, null);      
      order_listen_channel.exchangeDeclare(receive_exchange_name, "direct");      
      
      System.out.println(" [*] Waiting for order queue messages. To exit press CTRL+C");

      order_listen_channel.queueBind(receive_queue_name, receive_exchange_name, receive_routing_key_recorded);  
      order_listen_channel.queueBind(receive_queue_name, receive_exchange_name, saga_routing_key_orderresponse);      
      order_listen_channel.basicQos(0, 1, false);    
      
      final Consumer consumer = new DefaultConsumer(order_listen_channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          String message = new String(body, "UTF-8");
          
          System.out.println(" [x] Received '" + message + 
          "' On Key:" + envelope.getRoutingKey() + 
          " On Exchange:" + envelope.getExchange());
          
          try {            
            if (envelope.getRoutingKey().equals(saga_routing_key_orderresponse)) {
              process_orderresponse_event(message);
              
            }
            else if (envelope.getRoutingKey().equals(receive_routing_key_recorded)) {
              process_order_event(message);
              
            }                        

          } finally {
            System.out.println(" [x] Done");
            order_listen_channel.basicAck(envelope.getDeliveryTag(), false);
          }
        }
      };
      boolean autoAck = false;
      order_listen_channel.basicConsume(receive_queue_name, autoAck, consumer);
    } catch (Exception e) {
      System.out.println(" listen_order_queue Exception:" + e.getMessage());
    }
  }

  public void listen_topics() 
  {    
    try {
      topic_listen_channel = connection.createChannel();

      topic_listen_channel.exchangeDeclare(exchange_name_info, "topic");
      String queueName = topic_listen_channel.queueDeclare().getQueue();  
      
      topic_listen_channel.queueBind(queueName, exchange_name_info, map_topics.get("order"));
      topic_listen_channel.queueBind(queueName, exchange_name_info, map_topics.get("system"));

      System.out.println(" [*] Waiting for topic messages. To exit press CTRL+C");
      
      Consumer consumer = new DefaultConsumer(topic_listen_channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
          String message = new String(body, "UTF-8");
          System.out.println(" [x] Received '" + envelope.getRoutingKey() + "':'" + message + "'");

        }
      };
      topic_listen_channel.basicConsume(queueName, true, consumer);
      
    } catch (Exception e) {
      System.out.println(" listen_topics Exception:" + e.getMessage());
    }
   
  }

  public Boolean SendDataToEventStore(String event_type, String data) {   
    java.util.Calendar calendar = java.util.Calendar.getInstance();
    java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-dd-MM hh:mm:ss");
    String formated_date = df.format(calendar.getTime());

    JsonObject event_bus_json = new JsonObject();
    event_bus_json.addProperty("Aggregate", "Order");
    event_bus_json.addProperty("Topic", String.format("msdemo_topic.order.%s", event_type));
    //event_bus_json.addProperty("Timestamp", new SimpleDateFormat("ddMMyyyy HH:mm:ss").format(Calendar.getInstance().getTime()));
    event_bus_json.addProperty("Timestamp", formated_date);
    event_bus_json.addProperty("Version", "1.0");
    event_bus_json.addProperty("BUS_ExchangeName", receive_exchange_name);
    event_bus_json.addProperty("BUS_Queue", receive_queue_name);    
    event_bus_json.addProperty("BUS_RoutingKey", receive_routing_key_recorded);
    event_bus_json.addProperty("Data", data);
    System.out.println("SendData:" + event_bus_json.toString());    
    byte[] messageBodyBytes = event_bus_json.toString().getBytes();
    try {
      
      sender_channel.basicPublish(
                event_store_exchange_name, 
                event_store_routing_key,  
                new AMQP.BasicProperties.Builder().deliveryMode(2).build(), 
                messageBodyBytes);
      return true;
    } catch (IOException e) {
      System.out.println("SendData Exception:" + e.getMessage());
    }    
    return false;
  }
  public Boolean SendDataToOrderSaga(String event_type, String data) {           
    JsonObject event_bus_json = new JsonObject();    
    event_bus_json.addProperty("Event", event_type);        
    event_bus_json.addProperty("Data", data);
    System.out.println("SendDataToOrderSaga:" + event_bus_json.toString());    
    byte[] messageBodyBytes = event_bus_json.toString().getBytes();
    try {
      
      sender_channel.basicPublish(
                saga_exchange_name_order, 
                saga_routing_key_order,
                new AMQP.BasicProperties.Builder().deliveryMode(2).build(), 
                messageBodyBytes);
      return true;
    } catch (IOException e) {
      System.out.println("SendData Exception:" + e.getMessage());
    }    
    return false;
  }
  
  public void Close()
  {
    try {
      topic_listen_channel.close();  
    } catch (Exception e) {
      System.out.println(" topic_listen_channel.close() Exception:" + e.getMessage());
    }
    try {
      order_listen_channel.close();  
    } catch (Exception e) {
      System.out.println(" order_listen_channel.close() Exception:" + e.getMessage());
    }  
    try {
      sender_channel.close();  
    } catch (Exception e) {
      System.out.println(" sender_channel.close() Exception:" + e.getMessage());
    }    
  }
}



