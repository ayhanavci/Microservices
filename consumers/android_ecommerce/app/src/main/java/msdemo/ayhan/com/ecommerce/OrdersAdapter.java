package msdemo.ayhan.com.ecommerce;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

public class OrdersAdapter extends ArrayAdapter<OrderModel> {

    private ArrayList<OrderModel> dataSet;
    Context mContext;
    View mView;
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

    public OrdersAdapter(ArrayList<OrderModel> data, Context context, View view) {
        super(context, R.layout.listview_orders_row, data);
        this.dataSet = data;
        this.mContext=context;
        this.mView = view;
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).getId();
    }

    @Override
    public OrderModel getItem(int position) {
        return dataSet.get(position);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listview_orders_row, parent, false);
        TextView txt_product_name = rowView.findViewById(R.id.txt_product_name);
        TextView txt_order_date = rowView.findViewById(R.id.txt_order_date);
        TextView txt_order_status = rowView.findViewById(R.id.txt_order_status);
        TextView txt_extra_data = rowView.findViewById(R.id.txt_extra_data);

        OrderModel item = dataSet.get(position);
        txt_product_name.setText(item.getProductName());
        txt_order_date.setText(item.getOrderTime());

        if (item.getOrderStatus() == ORDER_STATUS_PENDING) {
            txt_order_status.setText("Pending");
        }
        else if (item.getOrderStatus() == ORDER_STATUS_CANCELED) {
            String message = "Fail";
            String extra_message = "";
            if (item.getCreditStatus() == PRICEANDSTOCK_STATUS_DENIED) {
                message = "Out of stock. ";
            }
            if (item.getCreditStatus() == CREDIT_STATUS_DENIED) {
                message = "Low credit";
                extra_message += String.format("%.1f - %.1f", item.getPrice(), item.getCredit());
                txt_extra_data.setText(extra_message);
            }
            txt_order_status.setText(message);
        }
        else if (item.getOrderStatus() == ORDER_STATUS_APPROVED) {
            txt_order_status.setText("Approved");
        }
        else if (item.getOrderStatus() == ORDER_STATUS_FINALIZED) {
            txt_order_status.setText("On Shipment");
        }

       return rowView;
    }

}