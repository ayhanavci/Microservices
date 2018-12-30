'''
e-Commerce Website. Users may; 
- Login, Logout
- View, Edit, Delete Products and Categories 
- View Customers, Edit Their Credits

2018 Ayhan AVCI. 
mailto: ayhanavci@gmail.com
https://lain.run
https://github.com/ayhanavci/
'''

from flask import Flask, request, flash, render_template, json, jsonify, session, abort
import requests
import os

app = Flask(__name__ )

proxy_address = os.environ["SERVICE_REGISTRY"] 

def login_request(user_name, password):
  response = requests.post("{}/product/login-user/".format(proxy_address), json={"UserName":user_name, "Password": password} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_categories_request():    
  response = requests.get("{}/product/get-categories/".format(proxy_address))  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_category_details_request(category_id):    
  response = requests.get("{}/product/get-category-details/".format(proxy_address), json={"CategoryId":category_id})
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code    

def add_category_request(category_name):
  response = requests.post("{}/product/add-new-category/".format(proxy_address), json={"Name":category_name} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code    

def edit_category_request(category_id, category_name):
  response = requests.post("{}/product/update-category/".format(proxy_address), json={"CategoryId":category_id, "Name":category_name} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code    

def delete_category_request(category_id):
  response = requests.post("{}/product/delete-category/".format(proxy_address), json={"CategoryId":category_id} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code   

def add_product_request(name, description, supplier, category_id, price, unitsinstock):
  response = requests.post("{}/product/add-new-product/".format(proxy_address), 
      json={"Name":name, "Description":description, "Supplier":supplier, "CategoryId":category_id, "Price":price, "UnitsInStock":unitsinstock} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code    

def edit_product_request(id, name, description, supplier, category_id, price, unitsinstock):
  response = requests.post("{}/product/update-product/".format(proxy_address), 
      json={"ProductId":id, "Name":name, "Description":description, "Supplier":supplier, "CategoryId":category_id, "Price":price, "UnitsInStock":unitsinstock} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code    

def delete_product_request(product_id):
  response = requests.post("{}/product/delete-product/".format(proxy_address), json={"ProductId":product_id} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code   

def get_all_products_request():    
  response = requests.get("{}/product/get-all-products/".format(proxy_address))  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_product_details_request(product_id):    
  response = requests.get("{}/product/get-product-details/".format(proxy_address), json={"ProductId":product_id})
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_products_of_category_request(category_id):
  response = requests.get("{}/product/get-products/".format(proxy_address), json={"CategoryId":category_id} )  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code   

def get_customers_request():    
  response = requests.get("{}/customer/get-all-users/".format(proxy_address))  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def get_user_info(user_name):    
  response = requests.get("{}/customer/get-user/{}".format(proxy_address, user_name))  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

def edit_user_credit_request(user_name, credit):
  response = requests.post("{}/customer/set-credit/".format(proxy_address), json={"UserName":user_name, "Credit":credit} )  
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
  response = requests.post("{}/manager/update-user/".format(proxy_address), json=updated_user)  
  if response.status_code == 200:
    return json.loads(response.content.decode('utf-8')), 200
  return "service call fail", response.status_code  

@app.route('/')
def home():  
  return render_template('index.html')
 
@app.route('/login', methods=['GET', 'POST'])
def login():
  if (check_login() == True):
    flash("User already logged in") 
    return home()
  if request.method == 'POST':
    result, code = login_request(request.form['username'], request.form['password'])
    if (request.form['username'] == "admin" and request.form['password'] == "pass"):
      session['username'] = request.form['username']
      session['logged_in'] = True    
    return home()
  else:
    return render_template('login.html')

@app.route("/logout")
def logout():
  session['logged_in'] = False
  session['username'] = ""
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

@app.route("/edit-category", methods=['GET', 'POST']) 
def edit_category():    
  if (check_login() == False):
    return home()
  if request.method == 'POST':
    edit_category_request(request.form['categoryid'], request.form['categoryname'])    
    return home()
  result, code = get_category_details_request(request.args.get('id'))
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      category_name = result['result']['Category'][2]
      return render_template('editcategory.html', category_id=request.args.get('id'), category_name=category_name)
  return home()

@app.route("/delete-category", methods=['GET', 'POST']) 
def delete_category():      
  if (check_login() == False):
    return home()
  if request.method == 'POST':
    delete_category_request(request.form['categoryid'])    
    return home()
  result, code = get_category_details_request(request.args.get('id'))
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      category_name = result['result']['Category'][2]
      return render_template('deletecategory.html', category_id=request.args.get('id'), category_name=category_name)
  return home()

@app.route("/add-category", methods=['GET', 'POST']) 
def add_category():      
  if (check_login() == False):
    return home()
  if request.method == 'POST':
    add_category_request(request.form['categoryname'])
  return render_template('addcategory.html')

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

@app.route("/products", methods=['GET']) 
def products():    
  if (check_login() == False):
    flash("User not logged in") 
    return home()
  result, code = get_all_products_request()    
  product_list = {}
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      product_list = result['result']['Products']      
  return render_template('products.html', products=product_list)

@app.route("/add-product", methods=['GET', 'POST']) 
def add_product():    
  if (check_login() == False):
    flash("User not logged in") 
    return home()     
  category_list = {}
  result, code = get_categories_request()      
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      category_list = result['result']['Categories']        
  if request.method == 'POST':
    add_product_request(
      request.form['productname'], request.form['description'], request.form['supplier'],
      request.form['select_category'], request.form['price'], request.form['unitsinstock'])        
  return render_template('addproduct.html', categories=category_list)  

@app.route("/edit-product", methods=['GET', 'POST']) 
def edit_product():    
  if (check_login() == False):
    return home()
  if request.method == 'POST':
    edit_product_request(
      request.form['productid'], request.form['productname'], request.form['description'], request.form['supplier'],
      request.form['select_category'], request.form['price'], request.form['unitsinstock']) 
    return home()  
  result, code = get_product_details_request(request.args.get('id'))
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):        
      result_cat, code_cat = get_categories_request()      
      category_list = {}
      if (code_cat == 200):
        if (str(result_cat['result']['Status']) == "Success"):  
          category_list = result_cat['result']['Categories']        
      return render_template('editproduct.html', product=result['result']['Product'], categories=category_list)
  return home()

@app.route("/delete-product", methods=['GET', 'POST']) 
def delete_product():    
  if (check_login() == False):
    return home()
  if request.method == 'POST':
    delete_product_request(request.form['productid'])    
    return home()
  result, code = get_product_details_request(request.args.get('id'))
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      product_name = result['result']['Product'][2]
      return render_template('deleteproduct.html', product_id=request.args.get('id'), product_name=product_name)
  return home()

@app.route("/customers", methods=['GET']) 
def customers():    
  if (check_login() == False):
    return home()
  result, code = get_customers_request()
  customer_list = {}
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):  
      customer_list = result['result']['Users']  
  print(customer_list)    
  return render_template('customers.html', customers=customer_list)  

@app.route("/edit-customer-credit", methods=['GET', 'POST']) 
def edit_customer_credit():    
  if (check_login() == False):
    return home()
  if request.method == 'POST':
    edit_user_credit_request(request.form['username'], request.form['credit'])
    return home()  
  result, code = get_user_info(request.args.get('id'))
  if (code == 200):
    if (str(result['result']['Status']) == "Success"):        
      result_cat, code_cat = get_user_info(request.args.get('id'))      
      if (code_cat == 200):
        if (str(result_cat['result']['Status']) == "Success"):            
          user_info = {}
          user_info['UserName'] = result['result']['User Info']['UserName']
          user_info['FullName'] = result['result']['User Info']['FullName']
          user_info['Credit'] = result['result']['User Info']['Credit']
          return render_template('editcustomercredit.html', customer=user_info)
  return home()

def check_login():
  if session.get('logged_in') is None:    
    return False
  if session['logged_in'] == False:      
    return False
  return True

if __name__ == "__main__":  
  app.secret_key = os.urandom(12)
  app.run(host="0.0.0.0", port=80, debug=True)
