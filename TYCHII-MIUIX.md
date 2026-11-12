# Tychii × Miuix — UI refactor notes

Tychii is a fork of Mihon, restyled with the Miuix component library
(HyperOS visual language, `top.yukonga.miuix.kmp`, v0.9.4) and renamed
from Mihon. This note records what changed, what was deliberately not
changed, and how to build.

## Miuix (v0.9.4)

- Maven: `top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`,
  `miuix-preference-android:0.9.4`, `miuix-blur-android:0.9.4`. Android-only
  artifacts (the `-android` suffix); the shared KMP artifacts are the same
  names without it.
- Shared minSdk is 24, so Tychii's minSdk 26 needed no change.
  `miuix-blur` declares minSdk 32; it IS used for the liquid-glass bottom
  bar (see below), with a manifest override + a runtime gate, so minSdk
  stays 26.
- Miuix does not depend on Material3 and is a full replacement design
  system, not a skin. It can coexist with Material3; each system needs its
  own theme wrapper.
- No initialization call. Wrap content in `MiuixTheme` with a
  `ThemeController`. Source: https://github.com/compose-miuix-ui/miuix
  (tag v0.9.4).

## Liquid glass (Wenku8Reader style)

- The floating navigation bar is a liquid-glass bar ported from
  [Wenku8Reader](https://github.com/TakaHoshino/Wenku8Reader) (which
  adapted it from SukiSU-Ultra / the miuix official example /
  Kyant0/AndroidLiquidGlass, Apache-2.0 — attribution headers are in
  each ported file). Effect stack in
  `eu.kanade.presentation.miuix` (`LiquidGlass.kt`, `LiquidInnerShadow.kt`,
  `liquid/…`): backdrop blur + vibrancy + lens refraction (AGSL
  RuntimeShader) + specular highlight + device-tilt light. Bar in
  `MiuixLiquidBottomBar.kt`, Tychii adapter in `TychiiLiquidBar.kt`.
- minSdk stays 26: the manifest uses
  `<uses-sdk tools:overrideLibrary="top.yukonga.miuix.kmp.blur" />` and
  all blur calls are gated behind `isMiuixGlassSupported` (API >= 32,
  `eu.kanade.presentation.theme.MiuixGlass.kt`). API 26–31: translucent
  solid bar (same look as before). API 32: blur + vibrancy + highlight.
  API 33+: full liquid glass with lens refraction.
- Accent is Miuix default blue (`MiuixTheme.colorScheme.primary`), not
  the app's Neko pink — user chose Miuix default blue for the bar.
- 5 tabs share the capsule by weight, 64.dp minimum per tab. Icon + 11.sp
  label when the "show navigation bar labels" setting is on, icon-only
  when off. Bar is 64.dp; content clearance is a single
  `LiquidNavBarClearance = 96.dp`.
- Backdrop wiring (two verified crash pitfalls, see Wenku8Reader's
  MainScaffold): the backdrop is applied via `Modifier.layerBackdrop`
  ONLY to the tab content, and the bar is a sibling in an outer Box
  OUTSIDE the recorded layer. Never combine
  `rememberLayerBackdrop { … drawContent() … }` with a wrapping
  `layerBackdrop`, and never put the bar inside the recorded layer —
  either makes hwui recurse infinitely in `RenderNode::prepareTreeImpl`
  and crash on launch.
- Style: one 3-state `UiPreferences.barStyle` (`BarStyle` enum in
  `domain/ui/model`, key `pref_bar_style`, default `LIQUID_GLASS`)
  controls BOTH the bottom bar and the title capsule: `OPAQUE` (solid
  surfaceContainer), `TRANSLUCENT` (surfaceContainer at 0.75 alpha),
  `LIQUID_GLASS` (blur + vibrancy + highlight; on API < 32 it resolves
  to `TRANSLUCENT`). Setting is a ListPreference in Settings →
  Appearance ("栏样式" / "Bar style"). The old boolean
  `bottomBarGlass` / `pref_bottom_bar_glass` was removed.
- `lens()` only works on `CornerBasedShape`, so the bar uses
  `RoundedCornerShape(32.dp)` (64.dp bar → capsule), not `CircleShape`.
- Deliberately NOT ported: Wenku8Reader's main-screen Miuix components
  (MiuixPage / MiuixSection grouped cards), its reader, and its settings
  pages. Settings / More stay Material3 per the user's instruction.

## Centered title capsule (2026-11-12)

- Each main tab's page name is shown in a centered Miuix capsule at the
  top (`MiuixCapsuleTitle` in `eu.kanade.presentation.miuix`), instead of
  Mihon's top-left toolbar title. It is a floating overlay in HomeScreen's
  outer Box (same layer as the bottom bar, OUTSIDE the recorded content
  layer — the glass capsule consumes the same backdrop, so it must never
  sit inside a tab's toolbar or content). It follows the same `BarStyle`
  as the bottom bar (opaque / translucent / liquid glass); its glass is
  blur + vibrancy + specular highlight only, no lens refraction, so the
  small pill's text is never distorted.
- The title comes from the current tab (`tabNavigator.current.options.title`)
  and updates on tab switch. Each tab's own toolbar title is hidden on
  phones only (gated on `!isTabletUi()` in LibraryToolbar, UpdatesAppBar,
  HistoryScreen, TabbedScreen); tablets keep their top-left titles and get
  no capsule. More has no toolbar title — its LogoHeader stays, so the
  capsule partly overlaps the Taiji icon at scroll top.
- The capsule hides while a tab is searching or selecting (the search
  field / selection counter would sit under it): each tab reports via
  `HomeScreen.showTitleCapsule` (Library: not selecting and no search
  query; Updates: not selecting; History/Browse: no search query; More:
  always shown). Sub-screens (manga detail, settings, reader) are
  unaffected — the capsule is under pushed screens in the navigation
  stack, and their own AppBars are untouched.
- Library's category name + manga-count pill are hidden with its title on
  phones (the capsule only shows "Library"); the category is still visible
  in the category tabs row.
- Library page must be one continuous background — only the title capsule
  and the bottom bar may read as separate elements. Three things used to
  split it: (1) the toolbar's pinned scroll behavior tinted it
  (surfaceTint overlay) as soon as the grid scrolled, (2) the category
  tab row drew a divider line, (3) the inner Library Scaffold re-applied
  the system-bar insets HomeScreen's outer Scaffold already applied, so
  the content started a status-bar height below the toolbar and ended a
  nav-bar height above the bottom bar. Fixed in LibraryTab (no scroll
  behavior on the toolbar; inner Scaffold takes
  `contentWindowInsets = WindowInsets(0, 0, 0, 0)`) and LibraryTabs
  (`divider = {}`, transparent container). The toolbar stays on the page
  background (surface == background in the Neko scheme), so it is
  invisible; only its action icons show.

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
- **Font:** M PLUS Rounded 1c (OFL) for every text style, letter
  spacing -0.15.sp (`Typefaces.appTypography`, font files in
  `res/font/mplus_rounded1c_*`, uploaded to GitHub 2026-11-12).
- **Library grid:** cover radius 12.dp with a 0.75.dp `#9D9D9D`
  outline (`MangaCover`), grid gaps 16.dp horizontal / 4.dp vertical,
  comfortable title below the cover in bodyMedium Medium (max 3
  lines), compact title on a black gradient scrim. Bookshelf covers
  also get a static glass gloss (`glassEffect = true`, only in
  `CommonMangaItem`): a diagonal linear gradient, white 0.18 alpha
  top-left fading to transparent with a faint black 0.08 edge at the
  bottom-right, like laminated glass. Static on purpose — covers are
  opaque, so there is no backdrop blur, and a tilt-following
  highlight would recompose the whole grid on every sensor update.
  The gloss is now on every manga cover in the app (not just the
  bookshelf): the `glassEffect` flag on `MangaCover` is enabled at
  all 12 call sites — bookshelf grid/list (`CommonMangaItem`),
  source browsing (same components), History, Updates, Upcoming,
  manga detail header (`MangaInfoHeader`, both layouts), tracker
  search, migration list, duplicate-manga dialog, and the shared
  `BaseMangaListItem`.
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

## Icon: Taiji (2026-11-12)

The launcher icon is now a Taiji (yin-yang) symbol, replacing the
black-and-white "T". Since 2026-11-12 the icon is the SAME in day and
night mode (user choice: keep it consistent) — the night-qualified
resources are byte-identical copies of the day ones, so dark mode
shows the white background and black-outlined Taiji, exactly like
day mode. The splash background (`@color/splash`) is white in both
modes too, and the More/About header (`LogoHeader`) shows the same
day Taiji in dark mode.

- Foreground (`ic_launcher_foreground.xml`, day / `drawable-night/`,
  night — identical): a Taiji — black half + white half with an
  S-curve, a white dot in the black half and a black dot in the white
  half, black outline (stroke 6).
- Background (`ic_launcher_background.xml` white /
  `drawable-night/` white — identical).
- Monochrome (`ic_launcher_monochrome.xml`): the Taiji half as a
  solid shape, for themed app icons.
- The debug source set mirrors main (foreground + background, day +
  night) so Android Studio's debug build shows the Taiji too.
- Splash (`ic_mihon_splash.xml`): uses the launcher foreground
  directly, with `@color/splash` as the background (white in both
  modes), so the splash looks exactly like the launcher icon.
- The More/About page header (`LogoHeader`) uses the launcher
  foreground, so it now shows the Taiji too (same day design in
  dark mode).
  It uses no tint, so the Taiji keeps its own black/white colors
  exactly like the launcher icon.

## Navigation bar labels (2026-11-12)

With labels on, the floating bar item is icon (28.dp) + 11.sp label.
It now has balanced vertical padding (8.dp top, 8.dp below the
label) so the icon and text no longer touch the bar's top and bottom
edges. Bar height with labels is 62.dp; content clearance is 94.dp.
The night Taiji outline is thicker (8 vs 6) so the symbol stays
readable on the black night background.

## Build

JDK 21, Android SDK API 37, then:

```sh
./gradlew :app:assembleDebug
```

Not verified in this environment: the local Gradle daemon fails to start
here (dispatch/Broken pipe before compilation), the same failure seen
with the earlier cloud build. Build on a local machine to verify.
