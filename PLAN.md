# E-ink Dashboard Launcher — Plan

## Goal
An Android home launcher for e-ink readers. The focus is a dashboard of widgets; an Apps button opens the app drawer.
Target devices: Fidibook, Boox, PocketBook, Meebook, Bigme, and regular phones.
(Kindle and Kobo don't run Android, so they're out of scope unless the device is rooted or flashed with Android.)

## Locked decisions
| Area | Decision |
|---|---|
| minSdk | 17 (Android 4.2) |
| Language/UI | Kotlin + XML Views (no Compose, which needs API 21+) |
| Libraries | Only the framework where possible; pin pre-API-21 AndroidX versions if needed (e.g. appcompat 1.6.x / core 1.12 → verify minSdk); OkHttp **3.12.x** + Conscrypt for TLS 1.2 on 4.x |
| Widgets | Built-in Kotlin classes implementing a `DashboardWidget` interface |
| Layout | User-editable grid, multiple pages, saved as JSON in SharedPreferences |
| Weather | Open-Meteo (no API key); city search via Open-Meteo geocoding |
| Locale | Persian + English, full RTL (`supportsRtl`, API 17 is the first RTL-capable release), Jalali + Gregorian dates |
| E-ink | No animations, monochrome high-contrast theme, coalesced redraws; on Boox, use the Onyx SDK via reflection when it's present |
| Distribution | Sideloaded APK through GitHub Releases |

## E-ink behavior
- **Ghosting:** none allowed. Every visible change triggers a full refresh. All updates are throttled and batched, so the screen flashes at most about once a minute.
- **Style:** minimal. White background, black text and thin dividers. No shadows, gradients or gray fills. Monochrome line icons. Light theme only on e-ink; dark mode is allowed on phones.
- **Color:** auto-detect; pure black and white on e-ink, with optional accents on color e-ink (Kaleido) and on phones.
- **Live widgets:** throttled, and only the changed region is redrawn. The clock updates once a minute. Pomodoro updates once a minute and once a second only in its final minute.
- **Always-on display:** when idle, the dashboard stays on screen with the screen kept on and the frontlight off. Best on a charging stand.
- **Fonts:** bundled Vazirmatn with bold weights, high contrast.
- **Input:** large touch targets (48dp or more). The device's page-turn buttons switch dashboard pages. Edit mode starts with a long-press; you move a widget by tapping it and then tapping the target cell, with no dragging.

## v1 widgets
1. Clock + date (Jalali/Gregorian, updates once a minute, no seconds)
2. Weather (current + 3-day forecast, refreshes every 1–3 h, cached offline)
3. Battery + Wi-Fi status (from broadcast receivers)
4. Continue reading (last-used reader app plus its launch intent; book title when the reader exposes it)
5. Notes / todo (local, stored in SQLite)
6. Pomodoro (AlarmManager-based, survives the screen sleeping)
7. API latency (user-configured URLs, periodic HEAD/GET timing, last-N history as text/sparkline)

## Architecture
```
app/
  launcher/      HomeActivity (HOME intent filter), AppDrawerActivity
  dashboard/     GridLayoutEngine, EditMode, PageAdapter, LayoutStore(JSON)
  widgets/       DashboardWidget interface + one package per widget
  refresh/       RefreshScheduler (AlarmManager), EinkRefresher (generic | Onyx)
  data/          SQLite helper, prefs, OkHttp client factory (Conscrypt)
  i18n/          JalaliCalendar, locale switcher, digit localization
```
`DashboardWidget`: `id`, `minSize/defaultSize`, `createView(ctx)`, `bind(view)`, `refreshIntervalMs`, `configActivity?`.
A single scheduler ticks the widgets and batches all their view updates into one redraw, which keeps e-ink flashing down.

## Phases / TODO
- [x] 0. Project skeleton (AGP 8.11.1, Gradle 8.14, minSdk 17, no AndroidX), HOME launcher, paged app drawer, e-ink refresh helper
  - [ ] Test on a Fidibook (Android 4.2–4.4) and a Boox; confirm the Onyx `ViewUpdateHelper` reflection call
- [x] 1. Grid engine (4x6 portrait / 6x4 landscape) + edit mode (long-press, then tap to move) + JSON persistence
  - Edit bar: Add, Remove, W±/H± resize, Page (add/delete), Done; Back also exits edit mode
  - Widgets are placeholders behind `DashboardWidget` / `WidgetRegistry` until phases 2–4
  - Verified on an API 36 emulator (render, select, remove, move, persist across restart); layout logic covered by JUnit tests
  - [ ] Verify on Android 4.2 (Fidibook) and the landscape layout (RTL mirroring checked on the emulator)
- [x] 2. Clock (Jalali/Gregorian), battery/Wi-Fi, notes checklist (SQLite)
- [x] 3. Weather (Open-Meteo, device location or city search, 2 h cache), API latency (5 min, last 10 results)
  - Networking changed: plain `HttpURLConnection` with TLS 1.2 forced on for API < 20 (no OkHttp/Conscrypt); weather falls back to HTTP if TLS fails on old root stores
  - [ ] Verify a real weather fetch and latency probe; the emulator couldn't reach Open-Meteo without the proxy
- [x] 4. Pomodoro (AlarmManager + full-screen inverted alert), continue reading (auto-picks an app whose package contains "fidibo", else a picker)
  - Reader apps don't expose the current book, so the widget shows the app, not the title
  - [ ] Let a pomodoro run out to check the alert screen; confirm Fidibo's package name on the device
- [x] UI pass: "paper" look (rules instead of boxes, light figures, uppercase section labels), flat outlined buttons, shared `Ui` kit and screen header
- [x] Edit bar has Setup: opens the selected widget's settings
- [x] Home redesign ("instrument panel"): self-sizing figures, latency bar charts per endpoint, pomodoro ring, day-progress meter, notebook-style notes; slim footer (Apps · page dots · battery/online) with swipe paging
  - Default page 1: clock, latency, weather, pomodoro; page 2: notes, reading. Layout key bumped to v3, so earlier saved layouts reset to these defaults
- [x] 5. Vazirmatn font (all text), in-app language switch (System/English/فارسی), always-on + frontlight off after idle (default 2 min), JSON backup export/import (system picker on 4.4+, Downloads file on 4.2/4.3), GitHub Actions build + tag releases, README
  - Verified on the API 36 emulator: Persian/RTL dashboard, edit mode and settings; backup export → import round trip
  - [ ] Push to GitHub to run CI for the first time; the release APK is debug-signed
  - [ ] Check frontlight dimming on real e-ink hardware (window brightness 0 may behave differently per device)

Build note: `~/.gradle/gradle.properties` routes Gradle through a proxy at 127.0.0.1:10808. When that proxy is down, build with `--offline`.

## Risks / open questions
- Gradle/AGP versions: current AGP still supports minSdk 17 builds, but D8 desugaring will be needed for some Kotlin/Java 8 APIs.
- Tablets on 4.x without Conscrypt-compatible ABIs: fall back to plain HTTP for Open-Meteo where it's allowed.
- "Continue reading" book titles depend on each reader app; v1 shows only the app unless a reader exposes more.
- Fidibook runs Android 4.2–4.4, so API 17 is a hard requirement. It's the main test device.

## Other decisions
- Pomodoro end: a full-screen inverted notice, plus sound or vibration when the device has them.
- API latency: multiple URLs, ms + up/down status, checked every 5 min, last 10 results kept.
- Weather: location from the network where available, with manual city search as fallback. Shows current conditions + 3-day forecast, text-first.
- App drawer: a paged grid you flip through with no scrolling (scrolling causes ghosting).
- System status bar hidden (full-screen); time and battery come from the app's own widgets.
- Package: `com.arashrasoulzadeh.einkdash`.
- First run: one page prefilled with all v1 widgets.
- Grid: 4x6 in portrait (6x4 in landscape), auto-scaled. Portrait and landscape keep separate layouts.
- Continue reading: targets the Fidibo app first (launch it and show the last book where possible).
- Notes: a checklist entered by keyboard.
- Backup: export and import the layout and settings as a JSON file.
