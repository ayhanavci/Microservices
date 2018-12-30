/*

Web Services. Place order and Get Orders API.

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/

*/

package com.ms_demo;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Root resource (exposed at "PlaceOrder" path)
 */
@Path("/")
public class Order {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getIt() {
      System.out.println("Root");
      return "Place Order Root";
    }
    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public String GetTest() {
      System.out.println("test");
      return "test";
    }
    @POST
    @Path("/get-orders")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response GetOrders(String orders_query_json) {
      System.out.println("GetOrders: " + orders_query_json);  
      String result = "{\"result\":\"Fail\"}";      
      if (VerifyGetOrderFields(orders_query_json)) {
        JsonObject data_json = new JsonParser().parse(orders_query_json).getAsJsonObject();                                                     
        System.out.println("GetOrders: Sending Data");          
        result = Main.viewer_database.GetOrders(data_json.get("Type").getAsString(), data_json.get("Id").getAsString()).toString();                
        System.out.println("GetOrders:[" + orders_query_json + "] Result:[" + result + "]");
      }
      else {
        System.out.println("GetOrders: Bad Json");  
        result = "{\"result\":\"Bad Json\"}";
      }
      System.out.println("GetOrders: End");  
      return Response.ok(result, MediaType.APPLICATION_JSON).build();             
    }

    @POST
    @Path("/place-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response PlaceOrder(String order_json) {    
      System.out.println("PlaceOrder Method: " + order_json);  
      String result = "{\"result\":\"Fail\"}";
      
      if (VerifyPlaceOrderFields(order_json)) {
        JsonObject message_json = new JsonParser().parse(order_json).getAsJsonObject();   
        System.out.println("PlaceOrder: Sending Data");  
        message_json.addProperty("Action", "Place Order");         
        message_json.addProperty("OrderID", UUID.randomUUID().toString()); 
        message_json.addProperty("Price", 0); 
        Boolean retval = Main.event_bus.SendDataToEventStore("place_order", message_json.toString()); 
        result = retval ? "{\"result\":\"Success\"}" : "{\"result\":\"Sending Data Failed\"}";        
        System.out.println("PlaceOrder Data Sent To ES:[" + order_json + "] Result:[" + result + "]");
      }
      else {
        System.out.println("PlaceOrder: Bad Json");  
        result = "{\"result\":\"Bad Json\"}";
      }
      System.out.println("PlaceOrder: End");  
      return Response.ok(result, MediaType.APPLICATION_JSON).build();      
    }
    public Boolean VerifyPlaceOrderFields(String order_json) 
    {
      try {
        JsonObject data_json = new JsonParser().parse(order_json).getAsJsonObject();                   
        String str_productid = "", str_customerid = "", str_timestamp = "";
        
        if (data_json.get("ProductId") != null)
          str_productid = data_json.get("ProductId").getAsString(); 
        if (data_json.get("CustomerId") != null)
          str_customerid = data_json.get("CustomerId").getAsString(); 
        if (data_json.get("TimeStamp") != null)
          str_timestamp = data_json.get("TimeStamp").getAsString();         

        if (str_productid.isEmpty() || str_customerid.isEmpty() || str_timestamp.isEmpty()) {
          System.out.println("VerifyPlaceOrderFields Empty Fields");
          return false;
        }
        return true;                            
      } catch (Exception e) {
        System.out.println(e.getMessage());  
      }
     
      return false;
    }
    public Boolean VerifyGetOrderFields(String orders_query_json) 
    {
      try {
        JsonObject data_json = new JsonParser().parse(orders_query_json).getAsJsonObject();                   
        String str_type = "", str_id = "";
        if (data_json.get("Type") != null)
          str_type = data_json.get("Type").getAsString();        
        if (data_json.get("Id") != null)
          str_id = data_json.get("Id").getAsString();        

        if (str_type.isEmpty() || str_id.isEmpty()) {
          System.out.println("VerifyGetOrderFields Empty Fields");
          return false;
        }
        return true;                            
      } catch (Exception e) {
        System.out.println(e.getMessage());  
      }
     
      return false;
    }
}
