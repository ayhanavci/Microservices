package msdemo.ayhan.com.ecommerce;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;


public class OrdersFragment extends Fragment {

    ArrayList<OrderModel> orders = new ArrayList<>();
    ListView orders_listview;
    OrdersAdapter orders_adapter;
    View mView;
    View mMainView;
    public OrdersFragment() {
        // Required empty public constructor
    }

    public static OrdersFragment newInstance() {
        OrdersFragment fragment = new OrdersFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_orders, container, false);

        mMainView = getActivity().findViewById(android.R.id.content);
        GetOrders();
        return mView;
    }
    private void GetOrders() {

        try {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            String user_name = sharedPref.getString(getString(R.string.saved_login_name), null);
            if (user_name == null) {
                Snackbar.make(mMainView, "Login first", Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
                return;
            }
            String proxy_ip = sharedPref.getString(getString(R.string.saved_proxy_ip), null);
            if (proxy_ip == null) {

            }

            JsonObject post_json = new JsonObject();
            post_json.addProperty("Type", "Customer");
            post_json.addProperty("Id", user_name);

            String [] params = {"order/get-orders/", "POST", post_json.toString()};


            new RestClientTask() {
                protected void onPostExecute(String result) {
                    try {
                        JsonObject response_json = new JsonParser().parse(result).getAsJsonObject();
                        String result_string = response_json.get("Result").toString().replaceAll("\"", "");
                        if (result_string.equals("Success")) {
                            String data_string = response_json.get("Data").toString();
                            JsonObject data_json = new JsonParser().parse(data_string).getAsJsonObject();
                            String status_string = data_json.get("result").toString().replaceAll("\"", "");
                            //{"GUID":"7a837981-4a1b-4a21-b226-8e2947d08e60","USERID":"jon","PRODUCT":"fcc6fe7b-1784-4f47-8efa-137b19a876ed",
                            // "PRODUCTNAME":"Hot Laptop","PRICE":19.0,"ORDERSTATUS":1,"STOCKSTATUS":3,"CREDITSTATUS":2,"TIME":"2018-24-12 01:36:53","CREDIT":5.0,"STOCK":43}
                            if (status_string.equals("Success")) {
                                JsonArray orders_json = data_json.get("Data").getAsJsonArray();
                                for (int i = 0; i < orders_json.size(); ++i) {
                                    JsonObject next_order = orders_json.get(i).getAsJsonObject();
                                    String Guid = next_order.get("GUID").getAsString();
                                    String UserId = next_order.get("USERID").getAsString();
                                    String ProductId = next_order.get("PRODUCT").getAsString();
                                    String ProductName = next_order.get("PRODUCTNAME").getAsString();
                                    Float Price = next_order.get("PRICE").getAsFloat();
                                    int OrderStatus = next_order.get("ORDERSTATUS").getAsInt();
                                    int StockStatus = next_order.get("STOCKSTATUS").getAsInt();
                                    int CreditStatus = next_order.get("CREDITSTATUS").getAsInt();
                                    String TimeString = next_order.get("TIME").getAsString();
                                    Float Credit = next_order.get("CREDIT").getAsFloat();
                                    int Stock = next_order.get("STOCK").getAsInt();
                                    orders.add(new OrderModel(
                                            Guid,
                                            UserId,
                                            ProductId,
                                            ProductName,
                                            Price,
                                            OrderStatus,
                                            StockStatus,
                                            CreditStatus,
                                            TimeString,
                                            Stock,
                                            Credit

                                    ));
                                }

                                orders_adapter = new OrdersAdapter(orders, getActivity(), getView());
                                orders_listview = mView.findViewById(R.id.listview_orders);
                                orders_listview.setAdapter(orders_adapter);
                            }
                            else {

                            }
                            Snackbar.make(mMainView, "GetOrders: " + status_string, Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();

                        }
                        else {
                            Snackbar.make(mMainView, "GetOrders Fail " + result, Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                        }
                    }
                    catch (Exception ex) {
                        Snackbar.make(mMainView, "GetOrders Fail (execute)" + ex.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();
                    }


                }
            }.execute(params);
        }
        catch (Exception ex) {
            String hede = ex.getMessage();

        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


}
