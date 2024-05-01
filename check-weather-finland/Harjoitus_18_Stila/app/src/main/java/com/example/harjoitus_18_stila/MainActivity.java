package com.example.harjoitus_18_stila;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Säätilasofta: ";

    private Queue<String> times;
    private Queue<Double> temperatures;

    private RequestQueue requestQueue;
    private Cache cache;
    private Network network;
    private String url;
    private String urlDefault;
    private String city;

    private EditText editTextCity;
    private Button buttonSearch;

    private TextView textViewInfo;
    private TextView textViewTemperature1;
    private TextView textViewTemperature2;
    private TextView textViewTemperature3;
    private TextView textViewTemperature4;
    private TextView textViewTemperature5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        times = new LinkedList<>();
        temperatures = new LinkedList<>();

        textViewInfo = findViewById(R.id.textViewInfo);
        textViewTemperature1 = findViewById(R.id.textViewTemperature1);
        textViewTemperature2 = findViewById(R.id.textViewTemperature2);
        textViewTemperature3 = findViewById(R.id.textViewTemperature3);
        textViewTemperature4 = findViewById(R.id.textViewTemperature4);
        textViewTemperature5 = findViewById(R.id.textViewTemperature5);

        cache = new DiskBasedCache(getCacheDir());
        network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();

        urlDefault = "https://opendata.fmi.fi/wfs/fin?service=WFS&version=2.0.0&request=GetFeature&storedquery_id=fmi::observations::weather::timevaluepair&place=";

        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.button_search);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(editTextCity.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Et ole syöttänyt paikkakuntaa", Toast.LENGTH_SHORT).show();
                } else {
                    dataSearch(editTextCity.getText().toString().toLowerCase());
                }
            }
        });
    }

    private void dataSearch(String givenCity) {
        Log.d(TAG, "dataSearch");
        url = urlDefault + givenCity;
        city = givenCity;
        requestAnswer();
    }

    private void updateTextViews() {
        Log.d(TAG, "updateTextViews");
        if (city.length() < 2 || times.size() < 5 || temperatures.size() < 5 ||temperatures == null || times == null) {
            Toast.makeText(MainActivity.this, "Ongelma: kokeile uudestaan", Toast.LENGTH_SHORT).show();
            return;
        }
        String s = city;
        Character c = Character.toUpperCase(s.charAt(0));
        s = c + s.substring(1);
        textViewInfo.setText(getResources().getString(R.string.label_info, s));

        s = times.poll();
        String new_s = s.substring(11, s.length() -1);
        Double d = temperatures.poll();
        textViewTemperature5.setText(getResources().getString(R.string.label_temperature5, new_s, d));

        s = times.poll();
        new_s = s.substring(11, s.length() -1);
        d = temperatures.poll();
        textViewTemperature4.setText(getResources().getString(R.string.label_temperature4, new_s, d));

        s = times.poll();
        new_s = s.substring(11, s.length() -1);
        d = temperatures.poll();
        textViewTemperature3.setText(getResources().getString(R.string.label_temperature3, new_s, d));

        s = times.poll();
        new_s = s.substring(11, s.length() -1);
        d = temperatures.poll();
        textViewTemperature2.setText(getResources().getString(R.string.label_temperature2, new_s, d));

        s = times.poll();
        new_s = s.substring(11, s.length() -1);
        d = temperatures.poll();
        textViewTemperature1.setText(getResources().getString(R.string.label_temperature1, new_s, d));
    }

    private void requestAnswer() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Saatiin response");
                        parseXML(new ByteArrayInputStream(response.getBytes()));
                        updateTextViews();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Response epäonnistui");
                        Toast.makeText(MainActivity.this, "Datan haku epäonnistui, kokeile syöttää toinen paikkakunta", Toast.LENGTH_SHORT).show();
                    }
                });
        requestQueue.add(stringRequest);
    }

    public void parseXML(InputStream is) {
        Log.d(TAG, "parseXML");
        String text = "";
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, null);

            int eventType = parser.getEventType();
            int counter = 0;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (counter > 1) {
                    return;
                }
                else {
                    String tagname = parser.getName();
                    switch (eventType) {
                        case XmlPullParser.TEXT:
                            text = parser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if (tagname.equalsIgnoreCase("time")) {
                                Log.d(TAG, "time: " + text);
                                if (times.size() == 5) {
                                    times.poll();
                                    times.add(text);
                                } else {
                                    times.add(text);
                                }
                            } else if (tagname.equalsIgnoreCase("value")) {
                                Log.d(TAG, "value: " + text);
                                boolean numeric = true;
                                numeric = text.matches("-?\\d+(\\.\\d+)?");
                                if (numeric) {
                                    if (temperatures.size() == 5) {
                                        temperatures.poll();
                                        temperatures.add(Double.parseDouble(text));
                                    } else {
                                        temperatures.add(Double.parseDouble(text));
                                    }
                                }
                            } else if (tagname.equalsIgnoreCase("shape")) {
                                Log.d(TAG, "Löydettiin shape-elementti");
                                counter++;
                            }
                            break;
                        default:
                            break;
                    }
                }
                eventType = parser.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        requestQueue.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        requestQueue.stop();
    }
}