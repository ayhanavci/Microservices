package com.ms_demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.PreparedStatement;

public class DatabaseOperations {
  private String host = System.getenv("DATABASE_IP");
  private String port = System.getenv("DATABASE_PORT");
  private String database_name = System.getenv("DATABASE_NAME");
  private String user_name = System.getenv("DATABASE_USERNAME");
  private String password = System.getenv("DATABASE_PASSWORD");
  private Connection connection;
  private String connection_string;
  
  public static final int ORDER_STATUS_PENDING = 0;
  public static final int ORDER_STATUS_CANCELED = 1;
  public static final int ORDER_STATUS_APPROVED = 2;  
  public static final int ORDER_STATUS_FINALIZED = 3;  

  public static final int PRICEANDSTOCK_STATUS_PENDING = 0;
  public static final int PRICEANDSTOCK_STATUS_RECEIVED = 1;
  public static final int PRICEANDSTOCK_STATUS_DENIED = 2;
  public static final int PRICEANDSTOCK_STATUS_APPROVED = 3;

  public static final int CREDIT_STATUS_PENDING = 0;
  public static final int CREDIT_STATUS_RECEIVED = 1;
  public static final int CREDIT_STATUS_DENIED = 2;
  public static final int CREDIT_STATUS_APPROVED = 3;

  DatabaseOperations()
  {
    connection_string = String.format("jdbc:postgresql://%s:%s/%s", host, port, database_name);
  }
  public Boolean connect() {
      
      try {
        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection(connection_string, user_name, password);                  
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println(e.getClass().getName()+": "+e.getMessage());
        System.exit(0);
      }
      System.out.println("Opened database successfully");
      return true;
   }
   public Boolean create_tables()
   {    
      Statement stmt = null;
      try {         
        System.out.println("Creating Orders table");
        if (connection.isClosed()) {
          System.out.println("Connection closed, reconnecting");
          if (!connect()) {
            System.out.println("Reconnect fail");
            return false;
          }
        }          
            
         stmt = connection.createStatement();
         String sql = "CREATE TABLE IF NOT EXISTS ORDERS " +
            "(ID SERIAL PRIMARY KEY        NOT NULL," +
            " GUID           TEXT       NOT NULL, " +
            " USERID         TEXT       NOT NULL, " +                        
            " PRODUCT        TEXT       NOT NULL, " +
            " PRICE          FLOAT      NOT NULL, " +
            " ORDERSTATUS    INT        NOT NULL, " +        
            " STOCKSTATUS    INT        NOT NULL, " +          
            " CREDITSTATUS   INT        NOT NULL, " +      
            " CREDIT         FLOAT      NOT NULL, " +    
            " STOCK          INT        NOT NULL, " +    
            " PRODUCTNAME    TEXT       NOT NULL, " +      
            " TIME           TEXT       NOT NULL)";
         stmt.executeUpdate(sql);
         stmt.close();        
         
      } catch ( Exception e ) {
         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
         System.exit(0);
      }
      System.out.println("Table created successfully");
      return true;
   }
   public Boolean InsertOrder(String str_orderid, String str_productid, String str_customerid, String str_price, String str_timestamp, String str_product_name) {
    Float price;
    try {
      price = Float.parseFloat(str_price);
    } catch (NumberFormatException e) {
      System.out.println(e.getMessage());
      return false;
    }

    PreparedStatement stmt = null;
    
    try {         
      System.out.println("Inserting Order");
      if (connection.isClosed()) {
        System.out.println("Connection closed, reconnecting");
        if (!connect()) {
          System.out.println("Reconnect fail");
          return false;
        }
      }          
                 
       String sql = "INSERT INTO ORDERS(GUID, USERID, PRODUCT, PRICE, ORDERSTATUS, STOCKSTATUS, CREDITSTATUS, TIME, CREDIT, STOCK, PRODUCTNAME) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
       stmt = connection.prepareStatement(sql);
       stmt.setString(1, str_orderid);
       stmt.setString(2, str_customerid);
       stmt.setString(3, str_productid);        
       stmt.setFloat(4, price);
       stmt.setInt(5, ORDER_STATUS_PENDING);
       stmt.setInt(6, PRICEANDSTOCK_STATUS_PENDING);
       stmt.setInt(7, CREDIT_STATUS_PENDING);
       stmt.setString(8, str_timestamp); 
       stmt.setFloat(9, 0); 
       stmt.setInt(10, 0); 
       stmt.setString(11, str_product_name);
       System.out.println("ALERT ALERT" + str_timestamp + " ALERT ALERT");
       stmt.executeUpdate();
       stmt.close();        
       
    } catch ( Exception e ) {
       System.err.println( e.getClass().getName()+": "+ e.getMessage() );
       System.exit(0);
    }
    System.out.println("Order inserted successfully");
    return true;
   }   
  public void UpdateCredit(String order_id, Float credit, int state) {            
    String sql = "UPDATE orders SET credit=?, creditstatus=? WHERE guid=?";    
    PreparedStatement stmt = null;    
    try {         
      System.out.println("UpdateCredit id:" + order_id);
      if (connection.isClosed()) {
        System.out.println("Connection closed, reconnecting");
        if (!connect()) {
          System.out.println("Reconnect fail");          
          return;
        }
      }                    
      stmt = connection.prepareStatement(sql);      
      stmt.setFloat(1, credit);
      stmt.setInt(2, state);
      stmt.setString(3, order_id);
      stmt.executeUpdate();
    
      stmt.close();        
       
    } catch ( Exception e ) {
       System.err.println( e.getClass().getName()+": "+ e.getMessage() );              
       return;
    }
    System.out.println("UpdateCredit returned successfully");    
  }
  public void UpdatePriceAndStock(String order_id, int stock, Float price, int state) {        
    String sql = "UPDATE orders SET stock=?, stockstatus=?, price=? WHERE guid=?";
    PreparedStatement stmt = null;    
    try {         
      System.out.println("UpdatePriceAndStock id:" + order_id);
      if (connection.isClosed()) {
        System.out.println("Connection closed, reconnecting");
        if (!connect()) {
          System.out.println("Reconnect fail");          
          return;
        }
      }          
          
      stmt = connection.prepareStatement(sql);      
      stmt.setInt(1, stock);
      stmt.setInt(2, state);
      stmt.setFloat(3, price);
      stmt.setString(4, order_id);
      stmt.executeUpdate();
    
      stmt.close();        
       
    } catch ( Exception e ) {
       System.err.println( e.getClass().getName()+": "+ e.getMessage() );              
       return;
    }
    System.out.println("GetOrders returned successfully");    
  }
  public void UpdateOrderStatus(String order_id, int state) {        
    String sql = "UPDATE orders SET orderstatus=? WHERE guid=?";
    PreparedStatement stmt = null;    
    try {         
      System.out.println("UpdateOrderStatus id:" + order_id);
      if (connection.isClosed()) {
        System.out.println("Connection closed, reconnecting");
        if (!connect()) {
          System.out.println("Reconnect fail");          
          return;
        }
      }                    
      stmt = connection.prepareStatement(sql);      
      stmt.setFloat(1, state);      
      stmt.setString(2, order_id);
      stmt.executeUpdate();
    
      stmt.close();        
       
    } catch ( Exception e ) {
       System.err.println( e.getClass().getName()+": "+ e.getMessage() );              
       return;
    }
    System.out.println("UpdateOrderStatus returned successfully");    
  }
  public JsonObject GetOrders(String type, String id) {
    JsonObject return_json = new JsonObject();    
    PreparedStatement stmt = null;    
    try {         
      System.out.println("Getting Orders. Type:" + type + " id:" + id);
      if (connection.isClosed()) {
        System.out.println("Connection closed, reconnecting");
        if (!connect()) {
          System.out.println("Reconnect fail");
          return_json.addProperty("result", "DB Connection Error");
          System.out.println("GetOrders: DB Connection Error");
          return return_json;
        }
      }          
      String sql = "";
      if (type.equals("All"))
        sql = "SELECT * FROM ORDERS ORDER BY TIME";      
      else if (type.equals("Customer"))
        sql = "SELECT * FROM ORDERS WHERE USERID=? ORDER BY TIME";              
      else if (type.equals("Single"))
        sql = "SELECT * FROM ORDERS WHERE GUID=? ORDER BY TIME";              
      else {
        return_json.addProperty("result", "Invalid Query Type");
        System.out.println("GetOrders: Invalid Query Type:" + type );
        return return_json;
      }       
      stmt = connection.prepareStatement(sql);      
      if (type.equals("Customer") || type.equals("Single")) 
        stmt.setString(1, id);
      
      ResultSet query_results = stmt.executeQuery();
      JsonArray rows = new JsonArray();         
      return_json.add("Data", rows);
      while (query_results.next())
      {        
        JsonObject row = new JsonObject();
        row.addProperty("GUID", query_results.getString("GUID"));
        row.addProperty("USERID", query_results.getString("USERID"));
        row.addProperty("PRODUCT", query_results.getString("PRODUCT"));
        row.addProperty("PRODUCTNAME", query_results.getString("PRODUCTNAME"));
        row.addProperty("PRICE", query_results.getFloat("PRICE"));
        row.addProperty("ORDERSTATUS", query_results.getInt("ORDERSTATUS"));
        row.addProperty("STOCKSTATUS", query_results.getInt("STOCKSTATUS"));
        row.addProperty("CREDITSTATUS", query_results.getInt("CREDITSTATUS"));    
        row.addProperty("TIME", query_results.getString("TIME"));  
        row.addProperty("CREDIT", query_results.getFloat("CREDIT")); 
        row.addProperty("STOCK", query_results.getInt("STOCK")); 
        
        rows.add(row);          
      }
      stmt.close();        
       
    } catch ( Exception e ) {
       System.err.println( e.getClass().getName()+": "+ e.getMessage() );
       return_json.addProperty("result", e.getClass().getName()+": "+ e.getMessage());
       return return_json;
    }
    System.out.println("GetOrders returned successfully");
    return_json.addProperty("result", "Success");
    return return_json;
  }
}