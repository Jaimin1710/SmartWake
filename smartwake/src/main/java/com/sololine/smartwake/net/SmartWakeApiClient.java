package com.sololine.smartwake.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.model.SmartWakeRemoteModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SmartWakeApiClient {

    private static final String TAG = "SmartWakeApiClient";

    public static SmartWakeRemoteModel fetchConfigSync(Context context, String apiUrl) {
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return parseConfigJson(SmartWakeConfig.getLastConfigJson(context));
        }

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String rawText = response.toString().trim();
                String jsonText = rawText;

                // Try Base64 decoding
                try {
                    byte[] decoded = Base64.decode(rawText, Base64.DEFAULT);
                    String decodedStr = new String(decoded, "UTF-8");
                    if (decodedStr.startsWith("{")) {
                        jsonText = decodedStr;
                    }
                } catch (Exception ignored) {
                }

                Log.d(TAG, "Fetched remote config successfully: " + jsonText);
                SmartWakeConfig.setLastConfigJson(context, jsonText);

                // Show Toast Notification on UI thread for developer feedback
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Toast.makeText(context.getApplicationContext(), "SmartWake API Config Fetched!", Toast.LENGTH_SHORT).show();
                    } catch (Exception ignored) {
                    }
                });

                return parseConfigJson(jsonText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching remote config from API: " + e.getMessage());
        }

        // Offline fallback: parse cached config
        return parseConfigJson(SmartWakeConfig.getLastConfigJson(context));
    }

    public static SmartWakeRemoteModel parseConfigJson(String jsonString) {
        SmartWakeRemoteModel model = new SmartWakeRemoteModel();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return model;
        }

        try {
            JSONObject json = new JSONObject(jsonString);

            model.showLockscreen = json.optBoolean("show_lockscreen", true);
            model.displayMode = json.optString("display_mode", "CENTER_DIALOG");
            model.workerTimeHours = json.optInt("workertime_hours", 1);

            model.showCenterDialog = json.optBoolean("show_center_dialog", true);
            model.showAd = json.optBoolean("show_ad", true);
            model.showWeather = json.optBoolean("show_weather", true);
            model.showWallpapers = json.optBoolean("show_wallpapers", true);
            model.showNews = json.optBoolean("show_news", true);
            model.showSports = json.optBoolean("show_sports", true);
            model.showQuotes = json.optBoolean("show_quotes", true);

            // Native Ad Priority Arrays
            JSONArray adPriorityArr = json.optJSONArray("adspriority");
            if (adPriorityArr != null) {
                for (int i = 0; i < adPriorityArr.length(); i++) {
                    model.adsPriority.add(adPriorityArr.getInt(i));
                }
            }

            JSONArray adFailPriorityArr = json.optJSONArray("adsfailpriority");
            if (adFailPriorityArr != null) {
                for (int i = 0; i < adFailPriorityArr.length(); i++) {
                    model.adsFailPriority.add(adFailPriorityArr.getInt(i));
                }
            }

            model.admobNative = json.optString("admob_native", "");
            model.fbNative = json.optString("fb_native", "");

            JSONObject customObj = json.optJSONObject("custom_native");
            if (customObj != null) {
                model.customNativeTitle = customObj.optString("title", "");
                model.customNativeDesc = customObj.optString("description", "");
                model.customNativeIcon = customObj.optString("icon", "");
                model.customNativeCta = customObj.optString("cta", "");
                model.customNativeLink = customObj.optString("link", "");
            }

            // Quotes
            JSONArray quotesArr = json.optJSONArray("quotes");
            if (quotesArr != null) {
                for (int i = 0; i < quotesArr.length(); i++) {
                    model.quotes.add(quotesArr.getString(i));
                }
            }

            // News
            JSONArray newsArr = json.optJSONArray("news");
            if (newsArr != null) {
                for (int i = 0; i < newsArr.length(); i++) {
                    model.news.add(newsArr.getString(i));
                }
            }

            // Sports
            JSONObject sportsObj = json.optJSONObject("sports");
            if (sportsObj != null) {
                model.sportsCricket = sportsObj.optString("cricket", "");
                model.sportsFootball = sportsObj.optString("football", "");
            }

            // Weather
            JSONObject weatherObj = json.optJSONObject("weather");
            if (weatherObj != null) {
                model.weatherCity = weatherObj.optString("city", "");
                model.weatherTemp = weatherObj.optString("temp", "");
                model.weatherCondition = weatherObj.optString("condition", "");
            }

            // Read More Links
            JSONArray linksArr = json.optJSONArray("readmore_links");
            if (linksArr != null) {
                for (int i = 0; i < linksArr.length(); i++) {
                    model.readMoreLinks.add(linksArr.getString(i));
                }
            }

            // Wallpapers
            JSONArray wallArr = json.optJSONArray("wallpapers");
            if (wallArr != null) {
                for (int i = 0; i < wallArr.length(); i++) {
                    model.wallpapers.add(wallArr.getString(i));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing remote config JSON: " + e.getMessage());
        }

        return model;
    }
}
