package com.example.silend.weatherapp;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.Inflater;

/**
 * Created by silend on 10.03.2015.
 */
public class ForecastFragment extends Fragment {


    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute("Moscow");

        List<String> weekForecastList = null;
        try {
            weekForecastList = new ArrayList<>(Arrays.asList(weatherTask.get()));

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_text_view, weekForecastList);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
            listView.setAdapter(arrayAdapter);

        return rootView;
    }
        public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {


            private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

            private String getReadableDateString(long time){
                SimpleDateFormat shortFormat = new SimpleDateFormat("EEE MMM dd");
                return shortFormat.format(time);
            }
            private String formatHighLows(double high, double low) {
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            private String[] getWeatherDataFromJson(String forecastJsonStr, int numdays) throws JSONException {
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJSON = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJSON.getJSONArray(OWM_LIST);
                Time dayTime = new Time();
                dayTime.setToNow();
                int julianStartDate = Time.getJulianDay(System.currentTimeMillis(),dayTime.gmtoff);
                dayTime = new Time();
                String[] resultStr = new String[numdays];
                for (int i = 0; i <weatherArray.length();i++){
                    String day;
                    String description;
                    String highAndLow;
                    JSONObject dayForecast = weatherArray.getJSONObject(i);
                    long dateTime = dayTime.setJulianDay(julianStartDate + i);
                    day = getReadableDateString(dateTime);
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);
                    highAndLow = formatHighLows(high, low);
                    resultStr[i]= day + " - " + description + " - " + highAndLow;

                }
                for (String s : resultStr){
                    Log.v(LOG_TAG, "Forecast entry: " + s);
                }
                return resultStr;
            }

            @Override
            protected String[] doInBackground(String... params) {
                HttpURLConnection urlConnection = null;
                BufferedReader bufferedReader = null;
                String forecastJsonStr = null;
                String format = "json";
                String units = "metric";
                int numDays = 7;
                String[] weekForecast = new String[0];
                try

                {
                    final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                    final String QUERY_PARAM = "q";
                    final String FORMAT_PARAM = "mode";
                    final String UNITS_PARAM = "units";
                    final String DAYS_PARAM = "cnt";
                    Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                            .appendQueryParameter(QUERY_PARAM, params[0])
                            .appendQueryParameter(FORMAT_PARAM, format)
                            .appendQueryParameter(UNITS_PARAM, units)
                            .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                            .build();

                    URL url = new URL(builtUri.toString());
                    Log.v(LOG_TAG, "BUILT URI " + builtUri.toString());
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) return null;


                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        buffer.append(line + '\n');
                    }

                    if (buffer.length() == 0) return null;

                    forecastJsonStr = buffer.toString();
                    Log.e(LOG_TAG, forecastJsonStr);

                } catch (
                        IOException e
                        )

                {
                    Log.e(LOG_TAG, "Error ", e);
                }  finally

                {
                    if (urlConnection != null) urlConnection.disconnect();
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }
                }

                try {
                    weekForecast = getWeatherDataFromJson(forecastJsonStr, numDays);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return weekForecast;


            }


        }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.forecastfragment, menu);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("Moscow");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    }
