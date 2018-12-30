package msdemo.ayhan.com.ecommerce;

import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class RestClientTask extends AsyncTask<String, Void, String>
{

    @Override
    protected void onPreExecute() {
        super.onPreExecute();


    }
    @Override
    protected String doInBackground(String... params) {

        String proxy_address = MainActivity.proxy_server_ip;
        JsonObject return_object = new JsonObject();
        HttpURLConnection service_connection = null;
        try {
            String endpoint_address = String.format("%s/%s", proxy_address, params[0]);
            URL reverse_proxy_endpoint = new URL(endpoint_address);
            service_connection = (HttpURLConnection) reverse_proxy_endpoint.openConnection();
            service_connection.setRequestMethod(params[1]);
            service_connection.setDoInput(true);
            service_connection.setRequestProperty("Content-Type", "application/json");
            service_connection.setRequestProperty("Accept", "application/json");
            if (params[1].equals("POST")) {
                String post_json = params[2];
                service_connection.setDoOutput(true);
                service_connection.getOutputStream().write(post_json.getBytes());
            }
            int response_code = service_connection.getResponseCode();
            if (response_code == 200) {
                InputStream responseBody = service_connection.getInputStream();
                String data = Convert(responseBody);
                return_object.addProperty("Result", "Success");
                return_object.add("Data", new JsonParser().parse(data).getAsJsonObject());

            } else {
                return_object.addProperty("Result", "Fail");
                return_object.addProperty("Data", response_code);

            }

        }
        catch (Exception ex) {
            return_object.addProperty("Result", "Fail");
            return_object.addProperty("Data",  ex.getMessage());

        }
        finally {
            if (service_connection != null)
                service_connection.disconnect();

        }

        return return_object.toString();
    }
    public String Convert(InputStream inputStream) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,  "UTF-8"))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

    }


}