# Eink Dash

A home-screen launcher for Android e-readers (Fidibook, Boox, PocketBook, Meebook, Bigme) and phones.
It's a dashboard of widgets with an app drawer, built for e-ink: pure black on white, no animations, and a full refresh on every change so there's no ghosting.

- **Runs on Android 4.2+** (minSdk 17), Kotlin with plain framework views, no AndroidX
- **Widgets:** clock (Gregorian + Jalali), weather (Open-Meteo), battery & Wi-Fi, API latency, notes checklist, pomodoro, continue reading (Fidibo first)
- **Editable grid:** 4×6 portrait / 6×4 landscape, multiple pages. Long-press to edit, tap a widget, tap a cell to move it
- **Persian + English** with full RTL and the Vazirmatn font
- **Always-on:** keeps the screen on and turns the frontlight off after an idle spell
- **Backup:** export/import layouts, settings and notes as JSON

## Install

Download the APK from Releases, or build it:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then press Home and choose Eink Dash as the default launcher.

## Use

- **Long-press** the dashboard to edit: Add, Remove, Setup (widget settings), More (pages, Settings), W±/H± to resize, Done
- **Page-turn or volume keys** flip pages
- **Settings** (Edit → More): language, keep screen on, frontlight timeout, backup

## Release

Push a tag like `v0.2.0`. CI builds, tests and lints, then attaches the APK to a GitHub release.
The release APK is debug-signed, which is fine for sideloading; add a signing config before distributing more widely.

Font: [Vazirmatn](https://github.com/rastikerdar/vazirmatn), SIL Open Font License 1.1 (`app/src/main/assets/fonts/OFL.txt`).
