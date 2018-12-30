/*

Entry Point. Initializes.

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/

*/
package com.ms_demo;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

import com.ms_demo.EventBus;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    //public static final String BASE_URI = "http://localhost:8080/myapp/";
    public static final String BASE_URI = "http://0.0.0.0";
    public static EventBus event_bus;
    public static DatabaseOperations viewer_database;
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.ms_demo package
        final ResourceConfig rc = new ResourceConfig().packages("com.ms_demo");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));                                
        if (!RunApplication())
          server.shutdownNow();
        
        //System.in.read();        
    }   
    public static boolean RunApplication() {
      try {           
          viewer_database = new DatabaseOperations();        
          if (!viewer_database.connect()) {
            System.out.println("Cannot connect to database:");
            return false;
          }            
          if (!viewer_database.create_tables()) {      
            System.out.println("Create tables failed");
            return false;
          }
          event_bus = new EventBus(viewer_database);
          event_bus.run();            
                                  
       } catch (Exception e) {
          System.out.println("RunApplication exception: " + e.getMessage());
          return false;      
       }           
       return true;
           
    }
}

