package com.sololine.smartwake.net;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.sololine.smartwake.SmartWakeConfig;
import com.sololine.smartwake.model.SmartWakeRemoteModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SmartWakeApiClient {

    private static final String TAG = "SmartWake";

    public static SmartWakeRemoteModel fetchConfigSync(Context context, String apiUrl) {
        Log.d(TAG, "API CLIENT fetchConfigSync called. URL: " + apiUrl);
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            Log.d(TAG, "API CLIENT: URL is empty. Returning cached config.");
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
            Log.d(TAG, "API CLIENT HTTP response code: " + responseCode);

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

                Log.d(TAG, "API CLIENT decoded JSON: " + jsonText);
                SmartWakeConfig.setLastConfigJson(context, jsonText);
                SmartWakeConfig.setLastApiFetchTime(context, System.currentTimeMillis());

                return parseConfigJson(jsonText);
            }
        } catch (Exception e) {
            Log.e(TAG, "API CLIENT ERROR: " + e.getMessage(), e);
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

            // Custom Native Ads (Supports both JSONArray of objects or single JSONObject)
            JSONArray customArray = json.optJSONArray("custom_native");
            if (customArray != null) {
                for (int i = 0; i < customArray.length(); i++) {
                    JSONObject obj = customArray.optJSONObject(i);
                    if (obj != null) {
                        model.customNativeList.add(new SmartWakeRemoteModel.CustomNativeAd(
                                obj.optString("title", ""),
                                obj.optString("description", ""),
                                obj.optString("icon", ""),
                                obj.optString("cta", ""),
                                obj.optString("link", "")
                        ));
                    }
                }
            } else {
                JSONObject customObj = json.optJSONObject("custom_native");
                if (customObj != null) {
                    model.customNativeList.add(new SmartWakeRemoteModel.CustomNativeAd(
                            customObj.optString("title", ""),
                            customObj.optString("description", ""),
                            customObj.optString("icon", ""),
                            customObj.optString("cta", ""),
                            customObj.optString("link", "")
                    ));
                }
            }

            // Weather
            JSONObject weatherObj = json.optJSONObject("weather");
            if (weatherObj != null) {
                model.weatherCity = weatherObj.optString("city", "");
                model.weatherTemp = weatherObj.optString("temp", "");
                model.weatherCondition = weatherObj.optString("condition", "");
            }

            // Wallpapers
            JSONArray wallArr = json.optJSONArray("wallpapers");
            if (wallArr != null) {
                for (int i = 0; i < wallArr.length(); i++) {
                    model.wallpapers.add(wallArr.getString(i));
                }
            }

            Log.d(TAG, "API CLIENT parseConfigJson DONE: displayMode=" + model.displayMode + ", showLockscreen=" + model.showLockscreen + ", customAdsCount=" + model.customNativeList.size());
        } catch (Exception e) {
            Log.e(TAG, "API CLIENT ERROR parsing JSON: " + e.getMessage());
        }

        return model;
    }
}
