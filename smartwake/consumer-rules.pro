# SmartWake SDK Consumer ProGuard Rules
-keep class com.sololine.smartwake.** { *; }
-keepclassmembers class com.sololine.smartwake.** { *; }

# Preserve AdMob classes for reflection
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }

# Preserve TradPlus & Facebook Native classes for reflection
-dontwarn com.tradplus.**
-keep class com.tradplus.** { *; }
-dontwarn com.facebook.ads.**
-keep class com.facebook.ads.** { *; }
