/*

Event Store DB Operations. 
- Inserts Event Sourcing events into Record DB as described in CQRS pattern.

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/

*/

using System;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using MongoDB.Bson;
using MongoDB.Driver;
using System.Collections.Generic;

class DbOperations
{
  private MongoClient client;
  private IMongoDatabase database;
  private IMongoCollection<BsonDocument> collection;
    
  public bool Init()
  {
    Console.WriteLine("DbOperations::Init()");
    try {          
      client = new MongoClient("mongodb://event_store_service:event_store_pass@msdemo-db-event-store:27017/");    
      database = client.GetDatabase("msdemo");  
      collection = database.GetCollection<BsonDocument>("events");
    }
    catch (Exception ex) {
      Console.WriteLine("DbOperations::Init() failed: " + ex.Message);
      return false;
    }
    Console.WriteLine("DbOperations::Init() success");
    return true;
  }
  
  public bool InsertEvent(dynamic item)
  {   
    Console.WriteLine("DbOperations::InsertEvent() Data:[{0}]", item["Data"]);
    try {      
   
      var document = new BsonDocument
      {
        {"Aggregate", new BsonString(item.Aggregate.ToString())},
        {"Topic", new BsonString(item.Topic.ToString())},
        {"Timestamp", new BsonString(item.Timestamp.ToString())},
        {"Version", new BsonString(item.Version.ToString())},
        {"Data", new BsonString(item.Data.ToString())}
      };   
      collection.InsertOne(document);
    }
    catch (Exception ex) {
      Console.WriteLine("DbOperations::InsertEvent() failed: {0}", ex.Message);
      return false;
    }    
    return true;
  } 
  
}