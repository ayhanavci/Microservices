package msdemo.ayhan.com.ecommerce;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class LoginFragment extends Fragment {
    Button btn_login;
    EditText txt_username;
    EditText txt_password;
    View mView;
    View mMainView;

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
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
        mView = inflater.inflate(R.layout.fragment_login, container, false);
        mMainView = getActivity().findViewById(android.R.id.content);

        txt_username = mView.findViewById(R.id.txt_user_name);
        txt_password = mView.findViewById(R.id.txt_password);
        btn_login = mView.findViewById(R.id.sign_in_button);

        btn_login.setOnClickListener((View v) -> {
            OnLoginClick(v);
        });


        return mView;
    }

    private void OnLoginClick(View view) {
        String user_name = txt_username.getText().toString();
        String password = txt_password.getText().toString();
        try {
            JsonObject post_json = new JsonObject();
            post_json.addProperty("UserName", user_name);
            post_json.addProperty("Password", password);

            String [] params = {"customer/login-user/", "POST", post_json.toString()};


            new RestClientTask() {
                protected void onPostExecute(String result) {

                    JsonObject response_json = new JsonParser().parse(result).getAsJsonObject();
                    String result_string = response_json.get("Result").toString().replaceAll("\"", "");
                    if (result_string.equals("Success")) {
                        String data_string = response_json.get("Data").toString();
                        JsonObject data_json = new JsonParser().parse(data_string).getAsJsonObject();
                        String status_string = data_json.get("result").toString();
                        JsonObject status_json = new JsonParser().parse(status_string).getAsJsonObject();
                        String login_status = status_json.get("Status").toString().replaceAll("\"", "");
                        if (login_status.equals("Success")) {
                            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getString(R.string.saved_login_name), user_name);
                            editor.putString(getString(R.string.saved_login_password), password);
                            editor.commit();

                        }
                        else {

                        }
                        Snackbar.make(mView, "Login: " + login_status, Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();

                    }
                    else {
                        Snackbar.make(mView, "Login Fail " + result, Snackbar.LENGTH_LONG)
                                .setAction("No action", null).show();
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
