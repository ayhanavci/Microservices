/*

Event Store. 
- Inserts Event Sourcing events into Record DB as described in CQRS pattern.
- Broadcasts record events

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
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;

class Program
{
  // AutoResetEvent to signal when to exit the application.
  private static readonly AutoResetEvent waitHandle = new AutoResetEvent(false);
  static void Worker()
  {
    var queue_name = "msdemo_queue_event_store";
    var routing_key = "msdemo_routingkey_event_store";
    var exchange_name_event_store = "msdemo_exchange_event_store";    

    var user_name = "msdemo_usr";
    var password = "msdemo_pass";
    var hostname = "msdemo-event-bus";        
    var exchange_name_events = "msdemo_exchange_events";
    
    DbOperations dbHandler = new DbOperations();
    if (!dbHandler.Init())
      return;

    var factory = new ConnectionFactory() { UserName = user_name, Password = password, HostName = hostname };

    using (var connection = factory.CreateConnection())
    using (var channel = connection.CreateModel())
    {
      channel.ExchangeDeclare(exchange: exchange_name_event_store, type: "direct");
      channel.QueueDeclare(queue: queue_name,
                           durable: true,
                           exclusive: false,
                           autoDelete: false,
                           arguments: null);

      channel.QueueBind(queue: queue_name, exchange: exchange_name_event_store, routingKey: routing_key);
      channel.BasicQos(prefetchSize: 0, prefetchCount: 1, global: false);

      Console.WriteLine("Waiting for messages.");

      var consumer = new EventingBasicConsumer(channel);
      consumer.Received += (model, ea) =>
      {
        var body = ea.Body;
        var message = Encoding.UTF8.GetString(body);
        Console.WriteLine("Raw message: {0}", message);
        try
        {
          dynamic msg_items; ; 
          if (ParseAndValidateJson(message, out msg_items)) 
          {
            Console.WriteLine("Storing event. Topic:[{0}] Aggregate:[{1}] Version:[{2}] Timestamp:[{3}] Data:[{4}]", msg_items.Topic, msg_items.Aggregate, msg_items.Version, msg_items.Timestamp, msg_items.Data);
            dbHandler.InsertEvent(msg_items); //Store event

            using (var channelReply = connection.CreateModel()) //Second channel to send reply 
            {
              string bus_exchange_name = msg_items.BUS_ExchangeName.ToString();
              string bus_queue_name =  msg_items.BUS_Queue.ToString();
              string bus_routing_key =  msg_items.BUS_RoutingKey.ToString();
              
              dynamic response = new JObject();
              response.Data = msg_items.Data;
              response.Action = "Write";
              response.Result = "Success";        
              byte [] response_string = Encoding.UTF8.GetBytes(response.ToString());      
              Console.WriteLine("Sending Reply To: |{0}|{1}|{2}|{3}", bus_exchange_name, bus_routing_key, bus_queue_name, response.ToString());

              channelReply.ExchangeDeclare(exchange: bus_exchange_name, type: "direct");
              channelReply.QueueDeclare(queue: bus_queue_name,
                                  durable: true,
                                  exclusive: false,
                                  autoDelete: false,
                                  arguments: null);

              channelReply.QueueBind(queue: bus_queue_name, exchange: bus_exchange_name, routingKey: bus_routing_key);
              channelReply.BasicQos(prefetchSize: 0, prefetchCount: 1, global: false);
              
              var properties = channel.CreateBasicProperties();
              properties.Persistent = true;              

              channelReply.BasicPublish(exchange: bus_exchange_name,
                     routingKey: bus_routing_key,
                     basicProperties: properties,
                     body: response_string);
            }

            using (var channelSender = connection.CreateModel()) //Third channel to publish stored event as Topic
            {
              channelSender.ExchangeDeclare(exchange: exchange_name_events, type: "topic");
              dynamic response = new JObject();
              response.Data = msg_items.Data;
              response.Action = "Write";
              response.Result = "Success";        
              byte [] response_string = Encoding.UTF8.GetBytes(response.ToString());    

              Console.WriteLine("Publishing {0} {1}", msg_items.Topic.ToString(), response.ToString());

              var properties = channel.CreateBasicProperties();
              properties.Persistent = true;   
              
              channelSender.BasicPublish(exchange: exchange_name_events,
                                   routingKey: msg_items.Topic.ToString(),
                                   mandatory: false,
                                   basicProperties: properties,
                                   body: response_string);
            }
            
          }
        }
        catch (Exception ex)
        {
          Console.WriteLine("Exception: {0}", ex.Message);
        }

        channel.BasicAck(deliveryTag: ea.DeliveryTag, multiple: false);
      };
      channel.BasicConsume(queue: queue_name, autoAck: false, consumer: consumer);

      waitHandle.WaitOne();
    }
  }
  static bool ParseAndValidateJson(string message, out dynamic msg_items)
  {
    List<string> errors = new List<string>();

    msg_items = JsonConvert.DeserializeObject<dynamic>(message, new JsonSerializerSettings
    {
      Error = delegate (object sender, ErrorEventArgs args)
      {
        errors.Add(args.ErrorContext.Error.Message);
        args.ErrorContext.Handled = true;
      },
      Converters = { new Newtonsoft.Json.Converters.IsoDateTimeConverter() }
    });
    foreach (string error in errors)
    {
      Console.WriteLine(error);
    }
    if (msg_items.Topic == null) { Console.WriteLine("Empty Topic"); return false; }
    if (msg_items.Aggregate == null) { Console.WriteLine("Empty Aggregate"); return false; }
    if (msg_items.Version == null) { Console.WriteLine("Empty Version"); return false; }
    if (msg_items.Timestamp == null) { Console.WriteLine("Empty Timestamp"); return false; }
    if (msg_items.Data == null) { Console.WriteLine("Empty Data"); return false; }
    if (msg_items.BUS_ExchangeName == null) { Console.WriteLine("Empty Data"); return false; }
    if (msg_items.BUS_Queue == null) { Console.WriteLine("Empty Data"); return false; }
    if (msg_items.BUS_RoutingKey == null) { Console.WriteLine("Empty Data"); return false; }
    
    return errors.Count == 0;
  }
  static void Main(string[] args)
  {
    Console.WriteLine("1.0");
    Task.Run(() => { Worker(); });

    Console.CancelKeyPress += (o, e) => { waitHandle.Set(); };

    waitHandle.WaitOne();
    Console.WriteLine("Bye");
  }
}