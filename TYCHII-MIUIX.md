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
