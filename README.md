# MAXINFRA SiteCam

An enterprise field engineering Android application for **MAX INFRA (I) PRIVATE LIMITED**, powered by APRICO.

**MAXINFRA SiteCam** allows field engineers to take site photos as proof of work. Every photo permanently burns a verifiable watermark into each image showing:
- Site Location (configurable default and engineers' saved locations dropdown)
- GPS Coordinates (Latitude, Longitude) & Accuracy
- Timestamp & Date (formatted in Asia/Kolkata timezone)
- Engineer Name & Contact Phone Number

---

## Key Features

1. **Brand Identity & Splash Screen**
   - Official MAX INFRA (I) PRIVATE LIMITED branding.
   - Navy (`#0B1F3A`) background with Amber (`#F59E0B`) accent colors.
   - Smooth 3-second startup splash sequence.

2. **Camera & Live Watermark**
   - Fullscreen CameraX viewfinder with fast, low-latency live overlay.
   - Real-time GPS status indicator (Live GPS / Low Accuracy / Test Mode).
   - Torch/Flash toggle and camera switch (Rear / Front).
   - Hardware shutter sound and vibration feedback.

3. **Permanent Watermark Stamp**
   - High-contrast, dual-layer burned-in watermark overlay directly embedded into the JPEG pixels.
   - GPS EXIF metadata encoded directly into the JPEG file.

4. **Engineers' Saved Locations**
   - Site location dropdown dynamically populated with all previously recorded site locations across projects.
   - Quick search and custom site text input.

5. **Local Data Persistence & Gallery**
   - Offline-first architecture using Android Room database.
   - Photo gallery with filtering by site location, full-resolution preview, and multi-selection.
   - One-tap ZIP export with auto-generated CSV manifest and batch photo sharing.

6. **Engineer Profile & Settings**
   - Editable Engineer Name and Contact Phone Number.
   - Default site location preferences.
   - Storage cleanup and privacy controls.

---

## Tech Stack & Architecture

- **Platform:** Android (minSdk 24, targetSdk 36)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Camera:** Android Jetpack CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Location:** Google Play Services FusedLocationProviderClient
- **Database:** Room (SQLite) with Kotlin Coroutines & Flow
- **Preferences:** Jetpack DataStore / SharedPreferences
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles

---

## Building the Project

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or newer
- Android SDK 36

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

---

## Push / Connect to GitHub from Google AI Studio

To push or sync this project with your GitHub account:
1. In the **Google AI Studio** web interface, open the **Project Settings** or menu in the top right.
2. Select **Export** or **Push to GitHub**.
3. Authorize your GitHub account and select or create the target repository (e.g. `maxinfra-sitecam`).
4. Alternatively, download the project as a **ZIP** from AI Studio, or add your GitHub remote URL in your local Git terminal:
   ```bash
   git remote add origin https://github.com/<your-username>/maxinfra-sitecam.git
   git push -u origin main
   ```
