# Reel Blocker

Android app that blocks **Instagram Reels** and **YouTube Shorts** the moment they open — built to kill doomscrolling and protect study time.

## Features

- **Reel Blocking** — Instagram Reels + YouTube Shorts blocked via Accessibility Service
- **No Mercy Mode** — strict block with no "continue" option, auto-returns to home screen
- **Adjustable delay** — 3–60 second friction screen (when strict mode is off)
- **Study Timer** — 15/25/45/60 min presets with start / pause / reset
- **Reel Counter** — tracks how many reels were blocked vs watched while blocking was off
- **Fintech-dark UI** with DP-blue accent and bottom tab navigation

## Download

Get the latest APK from [Releases](../../releases) (`ReelBlocker.apk`).

## Build manually (no Gradle daemon needed)

```bash
bash build-apk.sh
```

Pipeline: `aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zipalign` → `apksigner`.
Requires JDK 17 and Android SDK (build-tools 34.0.0, platform android-34).

Output: `~/workspace/your_files/ReelBlocker.apk` (override `APK_OUT` in the script if needed).

## Setup on device

1. Install the APK (uninstall any older copy first — debug signatures differ between builds).
2. Open the app → **Blocking** tab → **Accessibility Service ON karo**.
3. In system Accessibility settings, turn **Reel Blocker** ON.
4. Done — opening Reels/Shorts now hits the block screen.

## Project structure

```
app/src/main/
  AndroidManifest.xml
  java/com/juno/reelblock/
    MainActivity.java      # 3-tab UI (Blocking / Study / Counter)
    BlockActivity.java     # friction screen shown on block
    ReelBlockService.java  # accessibility-based reels/shorts detection
    Prefs.java             # toggles + counters (SharedPreferences)
  res/
    layout/                # activity_main (tabs), activity_block
    drawable/              # rounded cards, buttons, tab bar
    values/                # dark + DP-blue theme
    mipmap-*/              # launcher icon
```

## Notes

- Detection targets the fullscreen reels/shorts **player only** — home feeds with reels shelves are never blocked.
- Debug-signed APK. For Play Store, build a release AAB with target API 36.
