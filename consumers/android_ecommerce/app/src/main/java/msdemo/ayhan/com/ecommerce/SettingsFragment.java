package msdemo.ayhan.com.ecommerce;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class SettingsFragment extends Fragment {
    Button btn_save;
    EditText txt_proxy_ip;
    View mView;
    View mMainView;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
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
        mView = inflater.inflate(R.layout.fragment_settings, container, false);
        mMainView = getActivity().findViewById(android.R.id.content);

        txt_proxy_ip = mView.findViewById(R.id.txt_proxy_ip);
        btn_save = mView.findViewById(R.id.save_settings_button);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String proxy_ip = sharedPref.getString(getString(R.string.saved_proxy_ip), null);
        if (proxy_ip != null)
            txt_proxy_ip.setText(proxy_ip);


        btn_save.setOnClickListener((View v) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.saved_proxy_ip), txt_proxy_ip.getText().toString());
            editor.commit();
            MainActivity.proxy_server_ip = txt_proxy_ip.getText().toString();
            Snackbar.make(mMainView, "Settings Saved", Snackbar.LENGTH_LONG)
                    .setAction("No action", null).show();
        });


        return mView;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


}
