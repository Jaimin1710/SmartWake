package com.sololine.smartwake.model;

import java.util.ArrayList;
import java.util.List;

public class SmartWakeRemoteModel {

    public boolean showLockscreen = true;
    public String displayMode = "CENTER_DIALOG"; // "FULL_SCREEN" or "CENTER_DIALOG"
    public int workerTimeHours = 1;

    public boolean showCenterDialog = true;
    public boolean showAd = true;
    public boolean showWeather = true;
    public boolean showWallpapers = true;
    public boolean showNews = true;
    public boolean showSports = true;
    public boolean showQuotes = true;

    // Ad Priorities: 1 = AdMob Native, 2 = Facebook Native, 3 = Custom Native
    public List<Integer> adsPriority = new ArrayList<>();
    public List<Integer> adsFailPriority = new ArrayList<>();

    public String admobNative = "";
    public String fbNative = "";

    // Custom Native Ad
    public String customNativeTitle = "";
    public String customNativeDesc = "";
    public String customNativeIcon = "";
    public String customNativeCta = "";
    public String customNativeLink = "";

    // Content Feeds
    public List<String> quotes = new ArrayList<>();
    public List<String> news = new ArrayList<>();
    public String sportsCricket = "";
    public String sportsFootball = "";

    // Weather
    public String weatherCity = "";
    public String weatherTemp = "";
    public String weatherCondition = "";

    // Read More Links (Cycled on click)
    public List<String> readMoreLinks = new ArrayList<>();

    // Dynamic Wallpapers
    public List<String> wallpapers = new ArrayList<>();
}
