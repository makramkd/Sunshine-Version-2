package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by admin on 5/6/16.
 */
public class ForecastFragment extends Fragment {

    // API key for open weather API
    public static final String APPID = "cae53585d01b06891ebf760030ea9500";

    // TAG to use in logs
    public static final String FORECAST_FRAG_TAG = ForecastFragment.class.getSimpleName();

    // URL information for OWM to build URIs
    public static final String BASE_URL = "http://api.openweathermap.org/data/2.5/";
    public static final String AUTHORITY = "api.openweathermap.org";
    public static final String FORECAST_PARAM = "forecast";
    public static final String DAILY_PARAM = "daily";
    public static final String Q_PARAM = "q";
    public static final String MODE_PARAM = "mode";
    public static final String UNITS_PARAM = "units";
    public static final String CNT_PARAM = "cnt";
    public static final String APPID_PARAM = "APPID";
    public static final String JSON_MODE = "json";
    public static final String METRIC_UNITS = "metric";

    private ArrayAdapter<String> forecastAdapter;
    private ListView listView;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        getActivity().getMenuInflater().inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        forecastAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast,
                R.id.list_item_forecast_textview, new ArrayList<String>());

        listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
                Toast.makeText(getActivity(), textView.getText().toString(), Toast.LENGTH_SHORT)
                    .show();
            }
        });

        AsyncTask<String, Void, String[]> fetchWeatherTask = new FetchWeatherTask();
        fetchWeatherTask.execute("94043,USA");

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Log.e(FORECAST_FRAG_TAG, "Refresh button clicked!");
                AsyncTask<String, Void, String[]> fetchWeatherTask = new FetchWeatherTask();
                fetchWeatherTask.execute("94043,USA");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Uri constructForecastUri(String qValue, String mode, String units, int numDays) {
        return new Uri.Builder().scheme("http")
                .authority(AUTHORITY)
                .appendPath("data")
                .appendPath("2.5")
                .appendPath(FORECAST_PARAM)
                .appendPath(DAILY_PARAM)
                .appendQueryParameter(Q_PARAM, qValue)
                .appendQueryParameter(MODE_PARAM, mode)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(CNT_PARAM, Integer.toString(numDays))
                .appendQueryParameter(APPID_PARAM, APPID)
                .build();

    }

    private double getMaxTemperatureForDay(String weatherJson, int dayIndex) throws JSONException {
        JSONObject jsonObject = new JSONObject(weatherJson);
        // get list array
        JSONArray array = jsonObject.getJSONArray("list");
        // get the object at the given day index
        JSONObject day = (JSONObject) array.get(dayIndex);
        // get the max temperature for that day
        Double maxTemp = day.getJSONObject("temp").getDouble("max");
        return maxTemp;
    }

    private String formatHighLows(double high, double low) {
        return Math.round(high) + "/" + Math.round(low);
    }

    private String getReadableDateString(long time) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd");
        return format.format(time);
    }

    private String[] getWeatherDataFromJson(String weatherJson, int numDays) throws JSONException {
        if (weatherJson == null) {
            return null;
        }

        // JSON elements
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(weatherJson);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        // now we work exclusively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;
            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String FETCH_WEATHER_TAG = FetchWeatherTask.class.getSimpleName();

        public FetchWeatherTask() {
        }

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri uri = constructForecastUri(params[0], JSON_MODE, METRIC_UNITS, 7);
                URL url = new URL(uri.toString());

                Log.e(FETCH_WEATHER_TAG, uri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(FETCH_WEATHER_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(FETCH_WEATHER_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, 7);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(String[] result) {
            if (result != null) {
                Log.d(FETCH_WEATHER_TAG, "Finished connecting to OpenWeather");
                forecastAdapter.clear();

                // use addall if we can (faster)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    forecastAdapter.addAll(result);
                } else {
                    for (String str : result) {
                        forecastAdapter.add(str);
                    }
                }
            }
        }
    }
}
