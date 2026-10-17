# Orbitwall — Complete Project Context & Architecture (Post-Sprint 2)

> **Purpose of this document**: Provide a comprehensive, self-contained technical briefing of everything currently built, tested, and running in this repository so any developer or AI agent can immediately continue development without missing context.

---

## 1. Executive Summary

- **Repository**: Cloned from [`https://github.com/itztakrim/Orbitwall`](https://github.com/itztakrim/Orbitwall) / Active Fork [`https://github.com/fahitakrim/Orbitwall`](https://github.com/fahitakrim/Orbitwall)
- **Local Directory**: `C:\Users\Hp\.gemini\antigravity\scratch\Orbitwall`
- **Application ID / Package**: `com.aistudio.aurawall.qpkmrt` (Source package: `com.example`)
- **Official App Name**: **ORBITWALL**
- **Target Platform**: Android (Min SDK 24, Target SDK 36, Compile SDK 36.1)
- **Target Device Tested**: Nothing Phone (2a) (`model: A142`, `serial: 00055349J000552`, Android 14/15)
- **Current Status**: **Production Ready. Complete Nothing OS design system (Dark & Light themes, Dot-matrix typography, Nothing Red/Blue accents, Canvas technical graphics). 100% Cloud-Streamed via jsDelivr CDN & GitHub Releases. Lightweight APK (~18 MB debug, <8 MB release), Coil offline cache, 3-tab dock (Home, Favorites, Settings), full Settings screen with cache management.**

---

## 2. Architecture & Design System (Nothing OS Overhaul)

1. **22 Curated Wallpapers Streamed from Cloud**:
   - Release: [`wallpapers-v1`](https://github.com/fahitakrim/Orbitwall/releases/tag/wallpapers-v1)
   - All 22 original full-resolution 4K/HD PNG wallpapers hosted on GitHub Releases.
   - All 22 thumbnails hosted in `cloud_catalog/thumbs/*.jpg` and served globally via jsDelivr CDN.
   - Master catalog [`wallpapers.json`](https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/wallpapers.json) committed and live on the `main` branch.
   - Dual-resolver architecture in `SplitBackendRepository.kt`: queries `itztakrim/Orbitwall` first, seamlessly falls back to `fahitakrim/Orbitwall`, with offline in-memory fallback.

2. **Nothing OS Theme & Aesthetics (`Theme.kt`, `Color.kt`, `Type.kt`)**:
   - **Dark Mode**: Background `#0B0B0C`, Surface Card `#151517`, Card Border `#262629`, Primary Text `#FFFFFF`, Secondary Text `#8E8E93`, Accent Red `#E51C23`.
   - **Light Mode**: Background `#F2F2F5`, Surface Card `#FFFFFF`, Card Border `#E2E2E7`, Primary Text `#111113`, Secondary Text `#6C6C70`, Accent Red `#E51C23`.
   - **Dot-Matrix Typography**: Section headings formatted in uppercase monospace dot-matrix with trailing Nothing Red accent dots (`EXPLORE WALLS •`, `FAVORITES •`, `APPEARANCE •`, `STORAGE •`, `ABOUT •`).
   - **Canvas Technical Graphics**: Dotted concentric blueprint rings (`TechnicalCircleGraphic`) rendered smoothly on headers and empty states.
   - **Nothing 4-Dot Brand Logo**: Top-left header features a 2x2 grid icon with the bottom-right dot highlighted in Nothing Red.

3. **Floating Bottom Navigation Dock (`MainApp.kt`)**:
   - 3 sleek tabs separated by vertical divider lines (`|`):
     - **Home**: Active state highlights with a white/contrast pill.
     - **Favorites**: Active state highlights with a Nothing Red (`#E51C23`) pill.
     - **Settings**: Active state highlights with a Nothing Blue (`#1E6BF1`) pill.
   - Haptic feedback on each tab switch.

4. **Settings Screen (`SettingsScreen.kt`)**:
   - **APPEARANCE •**: Dark / Light theme segmented pill selector (persisted to SharedPreferences) and Haptic feedback toggle.
   - **STORAGE •**: Dynamic live cache size calculator reading from `orbitwall_image_cache`, with one-tap red `CLEAR` button to instantly wipe Coil disk/memory cache. Displays download destination (`Pictures/Orbitwall`).
   - **ABOUT •**: Displays app version `ORBITWALL v1.0.0`, Developer `Takrim`, and intent action links to Facebook and X (Twitter) profiles.

5. **APK Size Shrinkage**:
   - Removed 29.6 MB of heavy bundled assets (`app/src/main/assets/wallpapers/` and `app/src/main/assets/thumbnails/`).
   - Debug APK bundle decreased from ~51 MB to 18.89 MB (Release build with R8 < 8 MB).

---

## 3. Technology Stack & Dependencies

| Layer | Technologies / Libraries | Versions |
|---|---|---|
| **Language & Tooling** | Kotlin, Gradle Wrapper, Android Gradle Plugin (AGP), Java 17 (Temurin) | Kotlin `2.2.10`, AGP `9.1.1`, Gradle `9.3.1` |
| **UI Framework** | Jetpack Compose, Material 3, PullToRefreshBox | Compose BOM `2024.09.00`, Material 3 `1.3.x` |
| **Image Loading & Cache** | Coil Compose with dedicated 150 MB DiskCache & MemoryCache | `io.coil-kt:coil-compose:2.7.0` |
| **Local Persistence** | AndroidX Room (with KSP code generation) | Room `2.7.0`, KSP `2.3.5` |
| **Networking & API** | Retrofit 2, OkHttp 3, Moshi (with codegen) | Retrofit `2.12.0`, OkHttp `4.10.0`, Moshi `1.15.2` |
| **Color Science** | AndroidX Palette KTX | `androidx.palette:palette-ktx:1.0.0` |
| **Async & Reactive** | Kotlinx Coroutines, StateFlow, SharedFlow | Coroutines `1.10.2` |
| **Testing** | JUnit 4, Robolectric, AndroidX Runner | JUnit `4.13.2`, Robolectric `4.16.1` |

---

## 4. Directory & File Structure

```text
Orbitwall/
├── cloud_catalog/
│   └── thumbs/                              # 22 lightweight JPG thumbnails (served via jsDelivr CDN)
├── wallpapers.json                          # Live JSON catalog of 22 wallpapers
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # App label: @string/app_name (ORBITWALL)
│   │   │   ├── res/values/strings.xml       # app_name: "ORBITWALL"
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt          # Edge-to-edge entry point + 150 MB Coil cache configuration
│   │   │   │   ├── data/
│   │   │   │   │   ├── SplitBackendRepository.kt # Fetches wallpapers.json from jsDelivr + offline fallback
│   │   │   │   │   ├── VirtualStorageService.kt  # Cloud-first catalog service
│   │   │   │   │   └── local/
│   │   │   │   │       ├── AppDatabase.kt        # Room DB definition
│   │   │   │   │       └── FavoriteWallpaperDao.kt # Room DAO for offline favorites
│   │   │   │   ├── model/
│   │   │   │   │   └── Wallpaper.kt         # Domain Wallpaper & Room FavoriteWallpaperEntity models
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainApp.kt           # 3-tab navigation container (Explore, Collections, Favorites)
│   │   │   │   │   ├── WallpaperViewModel.kt# Main ViewModel managing UI state, filters, favorites, downloads
│   │   │   │   │   ├── components/
│   │   │   │   │   │   └── WallpaperCard.kt # Staggered grid card with dynamic aspect ratio & heart button
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── OnboardingScreen.kt      # Screen 1: 3D angled carousel with Orbitwall branding
│   │   │   │   │   │   ├── HomeScreen.kt            # Screen 2: Staggered feed + Pull-to-Refresh + ORBITWALL
│   │   │   │   │   │   ├── CollectionsScreen.kt     # Curated category cards (AMOLED, Anime, Minimal, Nature)
│   │   │   │   │   │   ├── FavoritesScreen.kt       # Room-persisted offline favorites
│   │   │   │   │   │   ├── WallpaperDetailScreen.kt # Screen 3: Full-res preview & Lock/Home overlay toggle
│   │   │   │   │   │   └── AdminUploadScreen.kt     # Standalone admin upload UI (accessible via code if needed)
│   │   │   │   └── util/
│   │   │   │       └── WallpaperSystemHelper.kt # Sets wallpaper to Home/Lock screen & saves to Pictures/Orbitwall
│   │   └── test/
│   │       └── java/com/example/
│   │           ├── ExampleUnitTest.kt
│   │           └── ExampleRobolectricTest.kt # SDK 34 Robolectric verification for ORBITWALL
```

---

## 5. Live Cloud Endpoints

1. **Master Catalog Index**:
   ```
   https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/wallpapers.json
   ```
   *Force Refresh URL*:
   ```
   https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/wallpapers.json?t={timestamp}
   ```

2. **CDN Thumbnails (450px wide, ~35 KB each)**:
   ```
   https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/{id}.jpg
   ```

3. **Full-Resolution 4K/HD Wallpapers (GitHub Releases)**:
   ```
   https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/{id}.png
   ```

---

## 6. How to Build & Install

```powershell
# 1. Run unit tests
.\gradlew.bat testDebugUnitTest

# 2. Compile debug APK
.\gradlew.bat assembleDebug

# 3. Install on connected Android device
& "C:\Users\Hp\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r -d app\build\outputs\apk\debug\app-debug.apk

# 4. Launch app
& "C:\Users\Hp\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.aistudio.aurawall.qpkmrt -c android.intent.category.LAUNCHER 1
```
