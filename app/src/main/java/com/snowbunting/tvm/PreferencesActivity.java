package com.snowbunting.tvm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
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

public class PreferencesActivity extends PreferenceActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference button = findPreference("old_loadDataButton");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Toast.makeText(getActivity(),R.string.fetch_data,
                            Toast.LENGTH_SHORT).show();

                    Context context = getActivity();
                    final SharedPreferences prefs = getDefaultSharedPreferences(context);
                    final String event_key = prefs.getString("event_key", getString(R.string.default_key));

                    RequestQueue queue = Volley.newRequestQueue(getActivity());
                    String url =getString(R.string.url);

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
                                        Toast.makeText(getActivity(),
                                                getString(R.string.popup_no_reservation) + String.format(" '%s'", event_key),
                                                Toast.LENGTH_SHORT).show();
                                    } else {

                                        // Save data
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putStringSet("seats_claimed", claimed);
                                        editor.apply();

                                        // Popup
                                        Toast.makeText(getActivity(),
                                                R.string.popup_success,
                                                //String.format("%s", prefs.getStringSet("claimed", new HashSet<String>())),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Toast.makeText(getActivity(),
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

                    return true;
                }
            });

            Preference button_delete = findPreference("delete_button");
            button_delete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    final Context context = getActivity();

                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm)
                            .setMessage(R.string.delete_all)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putStringSet("seats_checked_in", new HashSet<String>());
                                    editor.putStringSet("seats_claimed", new HashSet<String>());
                                    editor.apply();

                                    Toast.makeText(getActivity(),R.string.deleted, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }
            });
        }
    }
}