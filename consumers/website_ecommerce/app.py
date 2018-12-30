'''
e-Commerce Website. Users may; 
- Login, Logout, Register, View & Edit User Information 
- View Products and Categories 
- Place Purchase Orders, View Status of Their Purchase Orders

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/
'''

from flask import Flask, request, flash, render_template, json, jsonify, session, abort
import requests
import os
import time
from time import localtime

app = Flask(__name__ )

proxy_address = os.environ["SERVICE_REGISTRY"] 

def get_categories_request():  
  response = requests.get("{}/product/get-categories/".format(proxy_address))  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_all_products_request():      
  response = requests.get("{}/product/get-all-products/".format(proxy_address))    
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_products_of_category_request(category_id):
  response = requests.get("{}/product/get-products/".format(proxy_address), json={"CategoryId":category_id} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code   
  
def login_request(user_name, password):
  response = requests.post("{}/customer/login-user/".format(proxy_address), json={"UserName":user_name, "Password": password} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_user_info(user_name):    
  response = requests.get("{}/customer/get-user/{}".format(proxy_address, user_name))  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_orders():    
  response = requests.post("{}/order/get-orders/".format(proxy_address), json={"Type":"Customer", "Id":session['username']})  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def order_product_request(id, name):    
  response = requests.post("{}/order/place-order/".format(proxy_address), 
          json={"ProductId":id, "ProductName":name, "CustomerId":session['username'], "TimeStamp":time.strftime("%Y-%d-%m %H:%M:%S", time.localtime())})  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def register_user(user_name, password, fullname, email):
  new_user = {
    "UserName": user_name, 
    "Password": password, 
    "FullName": fullname,
    "Email": email, 
    "Credit": "100" 
  }
  response = requests.post("{}/customer/add-user/".format(proxy_address), json=new_user)  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def update_user(user_name, password, fullname, email):
  updated_user = {
    "UserName": user_name, 
    "Password": password, 
    "FullName": fullname,
    "Email": email, 
    "Credit": "100" 
  }
  response = requests.post("{}/customer/update-user/".format(proxy_address), json=updated_user)  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

@app.route('/')
def home():  
  return render_template('index.html')
 
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
      result, code = login_request(request.form['username'], request.form['password'])
      app.logger.info('login_request: %s, code: %d ', result, code)
      if code == 200:        
        if result['result']['Status'] == "Success":
          session['username'] = request.form['username']
          session['logged_in'] = True            
      flash("Login result {} code: {}".format(result, code))
      return home()
    else:
      return render_template('login.html')

@app.route("/logout")
def logout():
  session['logged_in'] = False
  session['username'] = ""
  return home()

@app.route("/register", methods=['GET', 'POST']) 
def register_customer():    
  if request.method == 'POST':
    result, code = register_user(request.form['username'], request.form['password'], request.form['fullname'], request.form['emailaddress'])
    app.logger.info('register_user: %s, code: %d ', result, code)
    flash("Register result {} code: {}".format(result, code))
    return home()
  else:
    return render_template('register.html')

@app.route("/orders", methods=['GET']) 
def orders():    
  if (check_login() == False):
    flash("User not logged in") 
    return home()
  result, code = get_orders()    
  print(result)
  order_list = {}
  if (code == 200):
    print(result)
    if (str(result['result'] == "Success")):  
      order_list = result['Data']
  return render_template('orders.html', orders=order_list)

@app.route("/update-user", methods=['GET', 'POST']) 
def update_customer():    
  if request.method == 'POST':
    if session.get('logged_in') is not None:
      if session['logged_in'] == True:            
        result, code = update_user(session['username'], request.form['password'], request.form['fullname'], request.form['emailaddress'])
        app.logger.info('update_user: %s, code: %d ', result, code)
        flash("Update User result {} code: {}".format(result, code))    
    return home()
  else:
    if session.get('logged_in') is not None:
      if session['logged_in'] == True:        
        result, code = get_user_info(session['username'])
        app.logger.info('get_user_info: %s, code: %d ', result, code)    
        if code == 200:        
          if result['result']['Status'] == "Success":        
            app.logger.info('get_user_info: %s, code: %d ', result, code)
            flash("Get User Info result {} code: {}".format(result, code)) 
            return render_template('userinfo.html', 
              user_name=result['result']['User Info']['UserName'], 
              full_name=result['result']['User Info']['FullName'], 
              password=result['result']['User Info']['Password'],
              email=result['result']['User Info']['Email'],
              credit=result['result']['User Info']['Credit'])      
      flash("Update user not logged in") 
    return home()    

@app.route("/products", methods=['GET']) 
def products():    
  if (check_login() == False):
    flash("User not logged in") 
    return home()
  result, code = get_all_products_request()    
  product_list = {}
  print(product_list)
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      product_list = result['result']['Products']      
  return render_template('products.html', products=product_list)

@app.route("/view-products/<id>", methods=['GET']) 
def view_products(id):      
  if (check_login() == False):
    flash("User not logged in") 
    return home()
  result, code = get_products_of_category_request(id)    
  product_list = {}
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      product_list = result['result']['Products']      
  return render_template('products.html', products=product_list)

@app.route('/order-product/<id>/<name>', methods=['GET']) 
def order_product(id, name):  
  if (check_login() == False):
    flash("User not logged in") 
    return home()
  result, code = order_product_request(id, name)  
  print(result)
  category_list = {}
  if (code == 200):
    if (str(result['result']) == "Success"):          
      flash("Order Complete")
    else:
      flash("Order Fail:".format(str(result['result'])) )
  return home()

@app.route("/categories", methods=['GET']) 
def categories():   
  if (check_login() == False):
    flash("User not logged in") 
    return home()
  result, code = get_categories_request()  
  print(result)
  category_list = {}
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      category_list = result['result']['Categories']      
  return render_template('categories.html', categories=category_list)

def check_login():
  if session.get('logged_in') is None:    
    return False
  if session['logged_in'] == False:      
    return False
  return True

if __name__ == "__main__":
  app.secret_key = os.urandom(12)
  app.run(host="0.0.0.0", port=80, debug=True)
