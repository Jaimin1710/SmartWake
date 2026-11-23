package com.sololine.smartwake.model;

import java.util.ArrayList;
import java.util.List;

public class SmartWakeRemoteModel {

    public boolean showLockscreen = true;
    public String displayMode = "CENTER_DIALOG"; // "FULL_SCREEN" or "CENTER_DIALOG"
    public int workerTimeHours = 1; // Interval in MINUTES

    public boolean showCenterDialog = true;
    public boolean showAd = true;
    public boolean showWeather = true;
    public boolean showWallpapers = true;

    // Ad Priorities: 1 = AdMob Native, 2 = Facebook Native, 3 = Custom Native
    public List<Integer> adsPriority = new ArrayList<>();
    public List<Integer> adsFailPriority = new ArrayList<>();

    public String admobNative = "";
    public String fbNative = "";

    // Custom Native Ad List (Cycled 1 by 1)
    public static class CustomNativeAd {
        public String title = "";
        public String description = "";
        public String icon = "";
        public String cta = "";
        public String link = "";

        public CustomNativeAd() {}

        public CustomNativeAd(String title, String description, String icon, String cta, String link) {
            this.title = title != null ? title : "";
            this.description = description != null ? description : "";
            this.icon = icon != null ? icon : "";
            this.cta = cta != null ? cta : "";
            this.link = link != null ? link : "";
        }
    }

    public List<CustomNativeAd> customNativeList = new ArrayList<>();

    // Weather
    public String weatherCity = "";
    public String weatherTemp = "";
    public String weatherCondition = "";

    // Dynamic Wallpapers
    public List<String> wallpapers = new ArrayList<>();
}
