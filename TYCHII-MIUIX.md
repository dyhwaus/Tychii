# Tychii × Miuix — UI refactor notes

Tychii is a fork of Mihon, restyled with the Miuix component library
(HyperOS visual language, `top.yukonga.miuix.kmp`, v0.9.4) and renamed
from Mihon. This note records what changed, what was deliberately not
changed, and how to build.

## Miuix (v0.9.4)

- Maven: `top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`,
  `miuix-preference-android:0.9.4`. Android-only artifacts (the `-android`
  suffix); the shared KMP artifacts are the same names without it.
- Shared minSdk is 24, so Tychii's minSdk 26 needed no change.
  `miuix-blur` needs API 33 and is NOT used.
- Miuix does not depend on Material3 and is a full replacement design
  system, not a skin. It can coexist with Material3; each system needs its
  own theme wrapper.
- No initialization call. Wrap content in `MiuixTheme` with a
  `ThemeController`. Source: https://github.com/compose-miuix-ui/miuix
  (tag v0.9.4).

## What changed

### Dependencies

`presentation-core/build.gradle.kts` — added as `api` so the app module
sees them transitively:

```kotlin
api("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
api("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
```

### Theme

`app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt` —
`BaseTachiyomiTheme` now wraps its content in `MiuixTheme` as well as
`MaterialExpressiveTheme`, with a `ThemeController` following the same
dark/light mode. Miuix keeps its own default colors (its
`surfaceContainer` backs the floating navigation bar) rather than the
app's custom palettes.

### Floating navigation bar (phones)

`app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt` —
the phone layout no longer uses Material3 `NavigationSuiteScaffold`.
It uses Miuix `Scaffold` with a `FloatingNavigationBar` in the
`floatingToolbar` slot: a detached pill (50.dp corners, surfaceContainer
color, drop shadow) that floats above the content, centered, with a
26.dp bottom offset plus the navigation-bar inset.

- The five tabs (Library, Updates, History, Browse, More) are icon-only,
  matching Miuix's floating-bar design; the label is kept as the content
  description. Selection is shown by icon opacity (full for selected,
  40% for unselected), the same as Miuix's own item.
- The tab icons stay as `Painter`s, so the animated library icon is kept.
  Miuix's own `FloatingNavigationBarItem` only accepts an `ImageVector`,
  so `FloatingNavItem` mirrors it with a `Painter` instead.
- The Updates / Browse count badges are kept (M3 `BadgedBox` + `Badge`).
- The bar hides and shows with the existing `showBottomNav` events
  (slide + fade). Tab content gets an extra 84.dp bottom padding
  (52.dp bar height + 26.dp offset + shadow) so nothing hides behind it.
- Tablets keep the Material3 navigation rail, unchanged.
- (An icon-plus-label variant with a selected-tab indicator pill was
  tried and reverted on 2026-11-11 at the user's request.)

### Splash screen (black and white, 2026-11-11)

- `ic_mihon_splash.xml` now shows the Tychii launcher foreground
  (the white T, 108.dp) instead of the old Mihon icon.
- Splash background `@color/splash` changed from `accent_blue`
  (#0058A0) to pure black (#000000), so the splash is a white T on
  black. The old `ic_mihon` drawable is still used by the More-page
  logo and notification icons, untouched.

### Navigation bar labels setting (2026-11-11)

- New preference `UiPreferences.showNavBarLabels`
  (`pref_show_navbar_labels`, default **on**): Settings → Appearance
  → "显示导航栏文字" / "Show navigation bar labels".
- When on, the floating bar shows icon + label (11.sp); when off,
  icon only. The bottom content clearance follows (96.dp with
  labels, 84.dp without).

### KernelSU consistency

The bar follows KernelSU's manager app (also built on Miuix 0.9.4).
KernelSU's own bar is a custom liquid-glass implementation (backdrop
blur + lens refraction + vibrancy, via `miuix-blur`, minSdk 33) — that
effect is not available at Tychii's minSdk 26, so Tychii uses the
translucent `surfaceContainer` (0.75 alpha), which is KernelSU's own
non-blur fallback. (An icon-plus-label variant with a selected-tab
indicator pill was tried and reverted; the bar is icon-only.)

## Neko style (2026-11-12)

Tychii's UI now follows Neko (nekomangaorg/Neko), keeping the Miuix
floating navigation bar. Neko is Material3 Expressive; Tychii already
uses MaterialExpressiveTheme.

- **Color scheme:** Neko's palette is now the default theme
  (`NekoColorScheme`, `AppTheme.DEFAULT` → Neko, plus an explicit
  `AppTheme.NEKO` entry). Primary `#FF124A` light / `#FCFC98` dark,
  background `#FBFBFF` / `#1A1C1E`. The old default blue is still
  available by picking another theme.
- **Font (pending):** M PLUS Rounded 1c (OFL) is Neko's signature
  rounded font, but the `.ttf` files are binary and cannot be pushed
  with the text-only push tool. The font files are staged locally in
  `res/font/mplus_rounded1c_*`; they still need to be uploaded to
  GitHub (web upload) and `Typefaces.appTypography` wired into
  `TachiyomiTheme.kt` before the rounded font takes effect. Until
  then the app uses the default font.
- **Library grid:** cover radius 12.dp with a 0.75.dp `#9D9D9D`
  outline (`MangaCover`), grid gaps 16.dp horizontal / 4.dp vertical,
  comfortable title below the cover in bodyMedium Medium (max 3
  lines), compact title on a black gradient scrim.
- **Floating bar:** selected tab now uses the app primary color
  (Neko pink/yellow) instead of onSurfaceContainer; unselected uses
  onSurfaceVariant. The bar itself is unchanged (Miuix pill, labels
  toggle kept).
- **Ripple:** primary color at 0.6 alpha for all states.
- **Not done (and why):** Neko's Feed merge (Updates+History into
  one tab) and its edge-attached NavigationBar are not copied —
  Tychii keeps its 5 tabs and floating bar. The per-manga palette
  detail screen, the search top bar, and the settings restyle are
  also not done (settings stay original Mihon per the user's
  "不变动其他页面"). The reader is untouched.

The settings and More pages were restyled to Miuix on 2026-11-11 and
reverted the same day at the user's request ("不变动其他页面"):
`PreferenceScreen`, `PreferenceScaffold`, `MoreScreen`, and the
`SwitchPreferenceWidget` / `TextPreferenceWidget` /
`CheckboxPreferenceWidget` are back to Mihon's original Material3
versions. Only the home screen (floating navigation bar), the icon,
and the splash screen use Miuix / the new design. The "show
navigation bar labels" setting below is the one exception: it adds a
single switch row to the Appearance settings, and the home screen
reads it.

### Floating bar transparency

The floating bar's background is `surfaceContainer` at 0.75 alpha, so
content shows through it. A real frosted-glass blur would need
`miuix-blur`, which requires minSdk 33; the translucent color is the
effect without a minSdk bump.

### Rename: Mihon → Tychii

- `i18n/src/commonMain/moko-resources/base/strings.xml` — `app_name`
  is now `Tychii` (launcher label and app name).
- Tracker `User-Agent` headers now say `Tychii v...` instead of
  `Mihon v...` (Anilist, Bangumi, Comick, Kavita, Kitsu, Komga,
  MangaBaka, MangaUpdates, MyAnimeList, Shikimori).
- Package name, applicationId and code references to Mihon are untouched.

### Icon (redesigned)

The launcher icon is a new mark: a white bold "T" whose stem ends in a
bookmark V-notch, on a pure black background (`#000000`) with a soft
dark-grey glow (`#262626`) behind the mark. Strictly black and white,
no color.

- `app/src/main/res/drawable/ic_launcher_foreground.xml` — the T mark.
- `app/src/main/res/drawable/ic_launcher_background.xml` — black + glow.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — the T mark
  in white, for themed icons.
- The same T mark and black background are also in
  `app/src/debug/res/drawable/` (`ic_launcher_foreground.xml`,
  `ic_launcher_background.xml`). The debug source set overrides `main`,
  so a debug build (what Android Studio runs by default) was still
  showing Mihon's original icon until these were replaced too.

A rendered preview is in `~/workspace/imagine_media/` as
`media-generation-tychii-icon-bw-preview-0-*.webp`.

## Deliberately NOT migrated

- The app shell's other screens, top bars, dialogs and lists still use
  Material3. Only the home navigation shell moved to Miuix.
- The settings/preference widgets (`SwitchPreferenceWidget`,
  `CheckboxPreferenceWidget`, `SettingsItems`, `LabeledCheckbox`,
  `SectionCard`, `Slider`) were not swapped for Miuix's preference rows.
  Miuix preferences are complete rows (title + summary + control in one
  call) while Mihon's are split widgets, so that migration restructures
  the settings screens rather than swapping widgets. It is the natural
  next step.
- No `miuix-blur`: the floating bar has no blurred backdrop (solid
  `surfaceContainer`). Blur would need minSdk 33.
- `miuix-nav` was not adopted; Mihon's existing navigation stays.

## Build

JDK 21, Android SDK API 37, then:

```sh
./gradlew :app:assembleDebug
```

Not verified in this environment: the local Gradle daemon fails to start
here (dispatch/Broken pipe before compilation), the same failure seen
with the earlier cloud build. Build on a local machine to verify.
