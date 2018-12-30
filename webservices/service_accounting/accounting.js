/*

Accounting Web Service. 
- Connects to Event Bus and Listens to Order Saga status update events
- For each finalized order, records an Invoice and sends it to user (just logs really)
- Has /get-revenue method which 

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/

*/
var express = require('express');
var amqp = require('amqplib/callback_api');

var app = express();
var fs = require("fs");
var couchdb_connection_string = `http://${process.env.ACCOUNTINGDB_USERNAME}:${process.env.ACCOUNTINGDB_PASSWORD}@${process.env.DATABASE_IP}:5984`
var nano = require('nano')(couchdb_connection_string);
var db_name = `${process.env.DATABASE_NAME}`;

nano.db.list(function(err, body) {
  var found = false;
  body.forEach(function(db) {
    console.log(db);
    if (db == db_name) {
      found = true;
      console.log("found accounting db");      
    }
  });
  if (!found) {
    console.log("Creating accounting db");  
    nano.db.create(db_name);
  }
});

var accounting_db = nano.db.use(db_name);

app.get('/get-revenue', function (req, res) {
  var total_price = 0;
  accounting_db.list({include_docs: true}).then((body) => {
    body.rows.forEach((doc) => {               
      var invoice_date = new Date(Date.parse(doc.doc['Time']));
      var current_year = (new Date()).getFullYear();
      console.log("Invoice Year:%d. Current Year:%d", invoice_date.getFullYear(), current_year);     
      if (invoice_date.getFullYear() == current_year);                      
        total_price += parseFloat(doc.doc['Price']);
    });
    var retval = {'Revenue':total_price};   
    var return_json = JSON.stringify(retval); 
    console.log("Returning revenue result:%s. Total Invoices:%d", return_json, body.rows.length);     
    res.end(return_json);
  });
})

var event_bus_string = `amqp://${process.env.EVENT_BUS_USERNAME}:${process.env.EVENT_BUS_PASSWORD}@${process.env.EVENT_BUS_IP}`;
amqp.connect(event_bus_string, function(err, conn) {
  conn.createChannel(function(err, ch) {
    var saga_routing_key_order = "msdemo_routingkey_saga_order";
    var receive_exchange_name_order = "msdemo_exchange_order";
    var receive_queue_name = "msdemo_queue_accounting";

    ch.assertExchange(receive_exchange_name_order, 'direct', {durable: false});  
    ch.assertQueue(receive_queue_name, {exclusive: false, durable: true, autoDelete: false, arguments:null},  function(err, q) {  
      ch.bindQueue(receive_queue_name, receive_exchange_name_order, saga_routing_key_order); 
      ch.consume(q.queue, function(msg) {
        message_json = JSON.parse(msg.content);     
                 
        console.log("Message received from route: %s Data: '%s'", msg.fields.routingKey, msg.content.toString());        
        if (msg.fields.routingKey == saga_routing_key_order) {                    
          console.log("Item Event is %s Data is: %s", message_json['Event'], message_json['Data']);
          if (message_json['Event'].toString() == "OrderFinalized") {
            data_json = JSON.parse(message_json['Data']);               
            fields_json = JSON.parse(data_json['Data']);
            console.log("ProductId is: %s", fields_json['ProductId']);
            
            var order_invoice = { 
              OrderID: fields_json['OrderID'], 
              ProductId: fields_json['ProductId'], 
              CustomerId: fields_json['CustomerId'],
              Price: fields_json['Price'], 
              TimeStamp: fields_json['TimeStamp']
            };
            
            accounting_db.insert(order_invoice, function(err, body){
              if(!err){
                console.log("Invoice inserted for Order:%s. Sending Invoice to customer...", body);
              }
              
            });
          }
          
        }
      }, {noAck: true});
    });

  });
});

var server = app.listen(80, function () {
   var host = server.address().address
   var port = server.address().port
   console.log("Accounting app listening at http://%s:%s", host, port)
})
