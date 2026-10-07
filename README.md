# WarmUp Manager (Android)

**WarmUp Manager** is an Android account management and warmup protocol application built with **Kotlin, Jetpack Compose, Material 3, Room, WorkManager, and AccessibilityService** (min SDK 26, target SDK 34). It manages, tracks, and automates organic warmup routines for TikTok, Instagram, and YouTube accounts that you own on your Android device.

---

## 3-Stage Development Roadmap (Completed)

| Stage | Focus | Status |
|---|---|---|
| **Stage 1** | **Account Tracker + Warmup State Logic (No Automation)** | **Completed ✓** |
| **Stage 2** | **Manual Session Timer Overlay + WorkManager Notifications + Real-Time Dashboard** | **Completed ✓** |
| **Stage 3** | **Assist Mode (AccessibilityService Human Behavior Engine)** | **Completed ✓** |

---

## Full Architecture & Tech Stack

* **Language & UI:** Kotlin, Jetpack Compose, Material 3 (Dark Theme default)
* **Local Persistence:** Room Database (`AppDatabase`, `AccountDao`, `SessionDao`) with TypeConverters
* **Background Tasks:** WorkManager (`DailyReminderWorker`, `WarmUpWorkManager`)
* **Foreground Service:** `FloatingTimerService` with `SYSTEM_ALERT_WINDOW` floating stopwatch widget
* **Automation:** `WarmUpAccessibilityService` with `HumanBehaviorEngine`
* **Reactive Concurrency:** Kotlin Coroutines (`Dispatchers.IO`), `Flow`, `StateFlow`

---

## Stage 3: Assist Mode (Human Behavior Engine)

### 1. Weighted Random Reactions Matrix
Every video is re-rolled independently with zero repeating mechanical loops:
* **Instant Skip (~25%):** Swipes away after 1–3 seconds.
* **Quick Glance (~20%):** Watches 20–40% of duration, then swipes.
* **Partial Watch (~25%):** Watches 50–80%, then swipes.
* **Full Watch (~20%):** Watches to the end, occasionally looping once.
* **Rewatch (~10%):** Watches fully and loops 2–3 times.

### 2. Interaction Rules (Likes & Saves)
* **Selective Liking (6–10%):** Triggers strictly from **Full watch** or **Rewatch**. Skips and glances are **never** liked.
* **Delayed Action:** Random 1–4 second delay before engaging.
* **Double-tap vs Heart Button:** 50/50 mix between double-tapping center screen coordinates and tapping the like icon.
* **Quiet Gap:** Strict quiet gap of 3–8 videos between interactions (never acts on consecutive videos).
* **Save / Bookmark (2–4%):** Only performed after a like or rewatch.
* **Unlike Heuristic (~1%):** Rarely likes and unlikes shortly after to simulate human mis-taps.
* **Session Mood Scaling:** Randomly selects **Low**, **Medium**, or **High** engagement mood per session (low mood sessions can have 0 likes).
* **Niche Bias:** Matches entered niche keywords to prolong watch durations on matching content by 1.3x–1.6x.

### 3. Motion & Scrolling Kinetics
* **Dynamic Bézier Curves:** Swipes follow curved paths with variable speed (180–550ms) and distance (65–85% of screen height).
* **Natural Jitter:** Adds random X and Y wobble to start, control, and end coordinates.
* **Occasional Idling:** Random pauses (3–15 seconds) simulating human reading of captions or distractions.

### 4. Safety Tripwires & Auto-Stop
* Automatically scans the active window hierarchy for text matching:
  * `"captcha"`
  * `"slide to complete"`
  * `"action blocked"`
  * `"try again later"`
  * `"unusual activity"`
  * `"confirm your identity"`
* When detected, the engine **immediately kills all automated gestures**, halts the session, and triggers a high-priority system notification prompting manual user takeover.

---

## Manifest Permissions & Accessibility Config

### AndroidManifest.xml Declarations:
```xml
<!-- Notifications & Overlay -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Accessibility Service Declaration -->
<service
    android:name=".service.WarmUpAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### accessibility_service_config.xml:
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds" />
```

---

## Honest Technical Limitations & Platform Fragility

1. **App Updates Breaking Like / Save Selectors:**
   * TikTok, Instagram, and YouTube push frequent updates where native view IDs (e.g. `com.zhiliaoapp.musically:id/like_icon`) are renamed, obscured, or refactored into Compose/Flutter trees.
   * **How we handle this:** `WarmUpAccessibilityService` prefers content descriptions (`"Like"`, `"Bookmark"`) and relative coordinate double-taps rather than brittle static IDs. When target apps overhaul their layout, adjust coordinates in `WarmUpAccessibilityService.kt` (`performLikeGesture` and `performSaveGesture`).
2. **Missing Touch Telemetry:**
   * Android's `dispatchGesture()` dispatches synthetic events lacking hardware gyroscope variance, touch pressure variation (`pressure = 1.0`), and finger capacitance contact area (`size = 0.0`).
   * **Mitigation:** The Human Behavior Engine relies on randomized Bézier curves, variable swipe durations, and strict daily caps (max 20 likes, 8 saves) to avoid triggering high-frequency bot heuristics.
3. **OEM Background Killing:**
   * Aggressive battery managers (Samsung OneUI, Xiaomi MIUI) will kill background accessibility services if left unattended.
   * **Fix:** Disable battery optimizations for WarmUp Manager (**Settings → Apps → WarmUp Manager → Battery → Unrestricted**).
4. **Never Automate Through CAPTCHAs:**
   * Trying to automate puzzle sliders or CAPTCHAs causes instant algorithmic shadowbanning. The app stops immediately upon seeing them.

---

## How to Build and Install on Windows

### Prerequisites
* [Java JDK 17+](https://adoptium.net/) (set `JAVA_HOME`).
* [Android Studio](https://developer.android.com/studio) or Android Command-Line Tools.

### Step 1: Build the Debug APK
Open Command Prompt or PowerShell in this project folder:
```powershell
.\gradlew.bat assembleDebug
```
The output APK will be at:
`app\build\outputs\apk\debug\app-debug.apk`

### Step 2: Install via ADB
1. On your phone: **Settings → About phone → tap Build number 7 times** to enable Developer options.
2. In **Settings → Developer options**, enable **USB debugging**.
3. Connect your phone via USB and run:
   ```powershell
   adb devices
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Step 3: Enable Accessibility Permission on Phone
1. Open **Settings → Accessibility → Installed Apps** (or Downloaded Apps).
2. Select **WarmUp Manager** and toggle the permission to **ON**.
