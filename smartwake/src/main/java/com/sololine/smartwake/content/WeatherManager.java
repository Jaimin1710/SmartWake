package com.sololine.smartwake.content;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherManager {

    private static final String TAG = "WeatherManager";
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast?latitude=19.0760&longitude=72.8777&current_weather=true";

    public interface WeatherCallback {
        void onWeatherFetched(String temp, String condition);
    }

    public static void fetchWeatherAsync(WeatherCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(OPEN_METEO_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    in.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject current = json.optJSONObject("current_weather");
                    if (current != null) {
                        double temp = current.optDouble("temperature", 28.0);
                        int weatherCode = current.optInt("weathercode", 0);

                        String condition = parseWeatherCode(weatherCode);
                        String tempStr = Math.round(temp) + "°C";

                        Log.d(TAG, "Weather fetched: " + tempStr + ", " + condition);
                        if (callback != null) {
                            callback.onWeatherFetched(tempStr, condition);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Weather fetch error: " + e.getMessage());
            }
        }).start();
    }

    private static String parseWeatherCode(int code) {
        switch (code) {
            case 0:
                return "Clear & Sunny";
            case 1:
            case 2:
            case 3:
                return "Partly Cloudy";
            case 45:
            case 48:
                return "Foggy";
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 65:
                return "Rainy";
            case 95:
            case 96:
            case 99:
                return "Thunderstorm";
            default:
                return "Pleasant";
        }
    }
}
