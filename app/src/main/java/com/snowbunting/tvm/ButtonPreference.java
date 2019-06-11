
package com.snowbunting.tvm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class ButtonPreference extends Preference {

    public Button button;

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        button = view.findViewById(R.id.load_data_button);
        button.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), R.string.fetch_data,
                        Toast.LENGTH_SHORT).show();

                //Context context = getActivity();
                final SharedPreferences prefs = getDefaultSharedPreferences(getContext());
                final String event_key = prefs.getString("event_key", "None");

                RequestQueue queue = Volley.newRequestQueue(getContext());
                String url = getContext().getString(R.string.url);

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Set<String> claimed = new HashSet<>();

                                String regex = "<input type=\"checkbox\" name=\"seat\" " +
                                        "value=\"([A-E])([0-9]{2})\" id=\"[^\"]*\" " +
                                        "class=\"[^\"]*(claimed|paid)";
                                Pattern pattern = Pattern.compile(regex);
                                Matcher matcher = pattern.matcher(response);

                                while (matcher.find()) {
                                    claimed.add(matcher.group(1) + matcher.group(2));
                                }
                                if (claimed.isEmpty()) {
                                    Toast.makeText(getContext(),
                                            getContext().getString(R.string.popup_no_reservation) + String.format(" '%s'",
                                                    event_key),
                                            Toast.LENGTH_SHORT).show();
                                } else {

                                    // Save data
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putStringSet("seats_claimed", claimed);
                                    editor.apply();

                                    // Popup
                                    Toast.makeText(getContext(),
                                            R.string.popup_success,
                                            //String.format("%s", prefs.getStringSet("claimed", new HashSet<String>())),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(getContext(),
                                        R.string.popup_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                ) {
                    @Override
                    // Add data that will be submitted in the POST request
                    protected Map<String, String> getParams() {
                        Map<String, String> postMap = new HashMap<>();
                        postMap.put("day", event_key);
                        return postMap;
                    }
                };
                queue.add(stringRequest);
            }
        });

    }
}