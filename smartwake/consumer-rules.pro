# SmartWake SDK Consumer ProGuard Rules
-keepattributes Signature,InnerClasses,EnclosingMethod

# Preserve SmartWake Public APIs, Models & Workers
-keep class com.sololine.smartwake.** { *; }
-keepclassmembers class com.sololine.smartwake.** { *; }

# Preserve AdMob classes for reflection and direct helper
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keepclassmembers class com.google.android.gms.ads.** { *; }

# Preserve TradPlus & Facebook Native classes for reflection and R8 minification
-dontwarn com.tradplus.**
-keep class com.tradplus.** { *; }
-keep interface com.tradplus.** { *; }
-keepclassmembers class com.tradplus.** { *; }
-keepclassmembers interface com.tradplus.** { *; }

-dontwarn com.facebook.ads.**
-keep class com.facebook.ads.** { *; }
-keep interface com.facebook.ads.** { *; }
-keepclassmembers class com.facebook.ads.** { *; }

# Preserve WorkManager Worker
-keep class com.sololine.smartwake.worker.SmartWakeWorker { *; }
