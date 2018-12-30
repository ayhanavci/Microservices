package msdemo.ayhan.com.ecommerce;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.reflect.Array;
import java.util.ArrayList;


public class ProductsFragment extends Fragment {

    ArrayList<ProductModel> products = new ArrayList<>();
    ListView products_listview;
    ProductsAdapter products_adapter;
    View mView;
    View mMainView;
    public ProductsFragment() {
        // Required empty public constructor
    }

    public static ProductsFragment newInstance() {
        ProductsFragment fragment = new ProductsFragment();
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
        mView = inflater.inflate(R.layout.fragment_products, container, false);
        mMainView = getActivity().findViewById(android.R.id.content);
        GetProducts();
        return mView;
    }
    private void GetProducts() {

        try {
            String [] params = {"product/get-all-products/", "GET", ""};


            new RestClientTask() {
                protected void onPostExecute(String result) {
                    try {
                        JsonObject response_json = new JsonParser().parse(result).getAsJsonObject();
                        String result_string = response_json.get("Result").toString().replaceAll("\"", "");
                        if (result_string.equals("Success")) {
                            String data_string = response_json.get("Data").toString();
                            JsonObject data_json = new JsonParser().parse(data_string).getAsJsonObject();
                            String status_string = data_json.get("result").toString();
                            JsonObject status_json = new JsonParser().parse(status_string).getAsJsonObject();
                            String register_status = status_json.get("Status").toString().replaceAll("\"", "");
                            if (register_status.equals("Success")) {
                                //String products_string = status_json.get("Products").toString().replaceAll("\"", "");
                                JsonArray products_json = status_json.get("Products").getAsJsonArray();
                                for (int i = 0; i < products_json.size(); ++i) {
                                    JsonArray next_product = products_json.get(i).getAsJsonArray();
                                    Integer product_id = next_product.get(0).getAsInt();
                                    String product_code = next_product.get(1).toString().replaceAll("\"", "");;
                                    String product_name = next_product.get(2).toString().replaceAll("\"", "");;
                                    String product_description = next_product.get(3).toString().replaceAll("\"", "");;
                                    String product_supplier = next_product.get(4).toString().replaceAll("\"", "");;
                                    String product_category_code = next_product.get(5).toString().replaceAll("\"", "");;
                                    Float product_price = next_product.get(6).getAsFloat();
                                    Integer product_stock = next_product.get(7).getAsInt();
                                    String product_category_name = next_product.get(8).toString().replaceAll("\"", "");;
                                    products.add(new ProductModel(
                                            product_id,
                                            product_code,
                                            product_name,
                                            product_description,
                                            product_supplier,
                                            product_category_code,
                                            product_price,
                                            product_stock,
                                            product_category_name));


                                }

                                products_adapter = new ProductsAdapter(products, getActivity(), getView());
                                products_listview = mView.findViewById(R.id.listview_products);
                                products_listview.setAdapter(products_adapter);
                            }
                            else {

                            }
                            Snackbar.make(mMainView, "GetProducts: " + register_status, Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();

                        }
                        else {
                            Snackbar.make(mMainView, "GetProducts Fail " + result, Snackbar.LENGTH_LONG)
                                    .setAction("No action", null).show();
                        }
                    }
                    catch (Exception ex) {

                    }


                }
            }.execute(params);
        }
        catch (Exception ex) {

        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


}
