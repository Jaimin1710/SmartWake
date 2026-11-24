# SmartWake SDK 🚀 (`com.sololine.smartwake`)

SmartWake is a lightweight, high-performance Android SDK designed for **20+ apps**. It listens to screen-on events in the background, displays a customizable lock screen activity with `setShowWhenLocked(true)`, and supports interactive swipe gestures to unlock/dismiss.

---

## 📦 1. Installation via JitPack

### Step A: Add JitPack repository
Add JitPack to your project's `settings.gradle.kts` (or root `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

### Step B: Add Dependency
Add SmartWake SDK to your app's `build.gradle.kts` (or `build.gradle`):

```kotlin
dependencies {
    implementation("com.github.Jaimin1710.SmartWake:smartwake:1.7.0")
}
```

---

## ⚡ 2. Quick Integration (1-Line Initialization)

To start SmartWake with default settings, simply add this line in your `Application` class or `MainActivity`:

```java
// Java
SmartWake.init(this);
```

```kotlin
// Kotlin
SmartWake.init(this)
```

---

## ⚙️ 3. Customized Integration for 20+ Apps (Builder API)

Each app can customize the lock screen title, subtitle, icon, swipe direction, clock visibility, vibration, and event listeners:

### Java Integration:
```java
SmartWake.builder(this)
    .setTitle("Welcome Back!")
    .setSubtitle("Swipe up to unlock your device")
    .setIconResId(R.drawable.ic_custom_app_logo)
    .setSwipeDirection(SmartWakeConfig.SWIPE_UP)
    .setShowClock(true)
    .setVibrationEnabled(true)
    .setListener(new SmartWakeListener() {
        @Override
        public void onScreenOn() {
            Log.d("SmartWake", "LockScreen shown");
        }

        @Override
        public void onDismissed() {
            Log.d("SmartWake", "LockScreen dismissed by swipe");
        }

        @Override
        public void onServiceStarted() {
            Log.d("SmartWake", "Background Service Active");
        }

        @Override
        public void onServiceStopped() {
            Log.d("SmartWake", "Background Service Stopped");
        }
    })
    .start();
```

### Kotlin Integration:
```kotlin
SmartWake.builder(this)
    .setTitle("Custom App Title")
    .setSubtitle("Swipe up to dismiss")
    .setIconResId(R.drawable.ic_custom_app_logo)
    .setSwipeDirection(SmartWakeConfig.SWIPE_UP)
    .setShowClock(true)
    .setVibrationEnabled(true)
    .start()
```

---

## 🛠️ 4. Key SDK Features

1. **Background Screen-On Monitoring**: Dynamically registers `ACTION_SCREEN_ON` via a foreground/background service (`SmartWakeService`) and handles device reboots (`BOOT_COMPLETED`).
2. **LockScreen Activity**: Built with `setShowWhenLocked(true)` and `setTurnScreenOn(true)` (API 27+) and legacy window flags (`FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON`, `FLAG_KEEP_SCREEN_ON`, `FLAG_DISMISS_KEYGUARD`) for seamless display over lock screen.
3. **Interactive Swipe Gesture**: Drag up/down/left/right with real-time velocity tracking, alpha fade, and smooth spring-back or dismissal animation.
4. **Haptic Feedback & Animation**: Built-in subtle bounce animation on swipe hint and click vibration on unlock.
5. **Zero Heavy Dependencies**: Pure AndroidX AppCompat and Material library - zero bloat for client apps!

---

## 🔒 5. Permissions Handled Automatically

SmartWake SDK includes necessary permissions in its `AndroidManifest.xml` (merged automatically into host apps):
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`
- `USE_FULL_SCREEN_INTENT`
- `POST_NOTIFICATIONS` (for Android 13+)

---

## 🧪 6. Manual Testing / Direct Launch

You can trigger the SmartWake lock screen UI at any time for testing:

```java
SmartWake.showNow(context);
```

To stop background monitoring:

```java
SmartWake.stop(context);
```
