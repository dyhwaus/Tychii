# Tychii × Miuix — UI refactor notes

Tychii is a fork of Mihon, restyled with the Miuix component library
(HyperOS visual language, `top.yukonga.miuix.kmp`, v0.9.4) and renamed
from Mihon. This note records what changed, what was deliberately not
changed, and how to build.

Current version: **v0.1.2** (versionCode 41) — every user-visible page
except the reader now follows the Miuix style. The reader is untouched
(gesture-driven fullscreen viewer, highest risk).

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

## Liquid-glass bottom bar (Wenku8Reader style)

The floating navigation bar on phones is a liquid-glass bar ported from
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
  solid bar. API 32: blur + vibrancy + highlight.
  API 33+: full liquid glass with lens refraction.
- Accent is Miuix default blue (`MiuixTheme.colorScheme.primary`), not
  the app's Neko pink — user chose Miuix default blue for the bar.
- 5 tabs share the capsule by weight, 64.dp minimum per tab. The bar is
  64.dp tall with or without labels; content clearance is a single
  `LiquidNavBarClearance = 96.dp`.
- Labels: a preference `UiPreferences.showNavBarLabels`
  (`pref_show_navbar_labels`, default **on**, Settings → Appearance →
  "显示导航栏文字" / "Show navigation bar labels") toggles icon + 11.sp
  label vs icon only. The item has balanced vertical padding (8.dp top,
  8.dp below the label) so icon and text never touch the bar's edges.
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
- Performance (2026-11-12, user reported heavy lag while scrolling):
  two fixes. (1) Tilt throttle: the rotation sensor fires ~50x/s and
  `rememberDeviceTilt` writes state on every event, which recomposed
  the bar (twice) and the capsule that often. `rememberTiltBrightHighlight`
  now snaps the gravity vector to a 0.05 grid via `derivedStateOf`, so
  the highlight still follows real tilts but sensor noise no longer
  recomposes anything. (2) The bar's second, invisible glass layer
  (an accent-colored copy of the tab content, alpha 0f, existing only
  so the selected pill could refract the accent color inside it) ran
  a full duplicate glass pass — vibrancy + blur + lens on the whole
  bar, every frame — so it was removed; the pill now refracts the page
  backdrop directly (`liquid/CombinedBackdrop.kt` is now unused). The
  selected icon itself is still drawn in the accent color. The backdrop
  itself is still re-recorded every scroll frame (inherent to liquid
  glass); if lag remains, the next lever is the lens refraction.
- Deliberately NOT ported: Wenku8Reader's main-screen Miuix components
  (MiuixPage / MiuixSection grouped cards), its reader, and its settings
  pages.
- Tablets keep the Material3 navigation rail, unchanged.

## Centered title capsule

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
  no capsule. More has no toolbar title and no LogoHeader; its page starts
  with a 64.dp blank band so the first row doesn't slide under the capsule.
- The capsule hides while a tab is searching or selecting (the search
  field / selection counter would sit under it): each tab reports via
  `HomeScreen.showTitleCapsule` (Library: not selecting and no search
  query; Updates: not selecting; History/Browse: no search query; More:
  always shown). Sub-screens (manga detail, settings, reader) are
  unaffected — the capsule is under pushed screens in the navigation
  stack, and their own AppBars are untouched.
- The capsule itself follows the active library category: the Library tab
  reports "书架 · 分类名" through a `HomeScreen.setCapsuleTitle` channel
  (`capsuleTitleEvent` + `libraryCapsuleTitle` state in HomeScreen;
  default/system categories report just "书架"). The channel was
  briefly removed on 2026-11-12 when the user chose to keep the pill
  search box and let the capsule yield, then restored the same day
  when the user changed their mind: keep the capsule, drop the search
  box (see "Search pill" below).
- Library's category name + manga-count pill are hidden with its title on
  phones; the category is still visible in the category tabs row, which
  is now capsule-styled (2026-11-12): the tab row keeps its transparent
  container and no divider, but the underline indicator is gone and the
  selected category is a filled capsule in Miuix `surfaceContainer`
  with bold `onSurface` text, echoing the title capsule; unselected
  categories are plain text in `onSurfaceVariant`. The count `Pill`
  badge is unchanged. (`TabText` gained a defaulted `fontWeight`
  parameter for this; its other two call sites are unaffected.)
- Browse tabs as capsules (2026-11-12): the Browse tab's 来源 / 拓展套件 /
  迁移 row uses the same capsule style as the Library category tabs
  (Miuix surfaceContainer fill on the selected tab, full pill radius,
  no underline indicator or divider). Miuix's own TabRow was not used:
  it takes only plain strings, so the extensions tab's update-count
  badge would have been lost.
- Top-bar action icons removed on phones (2026-11-12): the capsule
  shares its 64.dp band with each tab's toolbar, and the action icons
  sat under it (Library filter, Updates filter/calendar/refresh,
  History clear-all, Browse overflow/help). On phones those actions
  are now hidden — the band holds only the capsule and the search
  icon (which does not overlap it, and the capsule hides while
  searching). Tablets keep all actions (no capsule there), and
  selection/action-mode icons are untouched (the capsule hides
  then). Consequence: on phones Library filtering, Updates filtering
  / upcoming / refresh, History clear-all, and the Browse overflow
  and migration help are no longer reachable from the top bar. The
  More page's LogoHeader Taiji icon was removed for the same reason
  (it sat directly under the capsule); About keeps its own
  LogoHeader, since it is a sub-screen with its own AppBar and no
  capsule.
- Search pill (2026-11-12 — removed the same day): on phones
  the main tabs' bare search icon was briefly replaced by a rounded
  search box — 48.dp high, full pill radius, `surfaceContainerHigh`
  background, search icon + hint text. `SearchToolbar` had gained an
  opt-in `pillSearch` parameter; only the Library, History and Browse
  (Extensions sub-tab) call sites passed it, and only on phones, so
  sub-pages kept the icon and tablets kept the icon plus their toolbar
  title. The pill owned the top band on those three phone tabs, so
  their title capsule yielded to it (their `showTitleCapsule` reports
  also required `isTabletUi()`), and the Library hint carried the
  category ("搜索" + category name), History's read "搜索历史". The
  user changed their mind the same day: keep the title capsule, drop
  the search box. The `pillSearch` parameter and its branch in
  `SearchToolbar` are gone, the three call sites are back to the
  magnifier icon with the default search hint, the three tabs'
  `showTitleCapsule` reports no longer require `isTabletUi()`, and
  the capsule category channel (`setCapsuleTitle`) is restored.
- Empty states on the main tabs are quiet now (2026-11-12): a local
  `QuietEmptyScreen` (`presentation/components/`) replaces the shared
  `EmptyScreen` at the seven main-tab call sites (whole library,
  per-category, Updates, History, Sources, Extensions, Migrate). It
  drops the random error face and gives the message more room
  (32.dp side padding, 32.dp above the action row); the buttons
  (getting-started guide, extension stores) and all trigger logic
  are unchanged. The shared `EmptyScreen` in presentation-core is
  untouched, so error states on sub-screens keep their face. The
  only copy change is Extensions: "Well, this is awkward" is now
  "No extensions yet" (`empty_extensions_quiet`, base + zh-rCN);
  other messages reuse their existing strings.
- Detail page cover palette (2026-11-12, Neko-style, kept quiet): the
  manga detail page now takes one color from its cover and uses it in
  only two places — a faint 0.25-alpha wash over the backdrop image
  and the active color of the action row (in-library / interval /
  tracking). Title, author, description and chapter rows keep their
  theme colors. The chain: the backdrop `AsyncImage` in `MangaInfoBox`
  decodes with `.allowHardware(false)` (Palette cannot read hardware
  bitmaps) and reports its drawable on success; the ui `MangaScreen`
  downscales it to 200px wide and runs `Palette`, picking via Neko's
  `getBestColor()` (adapted, with attribution header); the color lives
  in `MangaViewModel.vibrantColor` (in-memory only, re-extracted on
  each visit, no persistence, no setting switch); the presentation
  `MangaScreen` derives the display color with `rememberThemeColor`,
  which first clamps saturation to 0.4 (`Color.quietVibrant`, a Tychii
  addition — Neko has no such step and can still come out garish)
  and then applies Neko's luminance nudge (dark mode lightens toward
  white, light mode darkens toward black). New dependency
  `androidx.palette:palette:1.1.0`. If the cover fails to load, or no
  swatch qualifies, the page silently stays on the default theme
  colors. Scope deliberately kept small: no chapter-row / TopBar /
  ripple / Snackbar tinting, no global theme change.
- More page grouped cards (2026-11-12): the More page now uses Miuix
  grouped cards — one `Card` per group (`SwitchPreference` for the
  two switches, `ArrowPreference` for the five navigation rows), no
  dividers between rows, 12.dp between groups, 12.dp side margins.
  Entries and click behaviour are unchanged, and Support us / About /
  Help stay removed.
- Seam unification (2026-11-12, from the user's Library screenshot):
  the top toolbar band was pure black (the shared AppBar paints Miuix
  `surface` by default) while the page is the theme background. The
  four tab toolbars (Library/Updates/History/Browse) now pass
  `MaterialTheme.colorScheme.background` (SearchToolbar gained a
  `backgroundColor` passthrough; the AppBar default is unchanged so
  pushed screens keep their look, and action-mode tint is kept by
  passing null while selecting). The list-item `HorizontalDivider`
  under every row is removed on the Library/Updates/History tabs
  (MangaListItem gained `showDivider`, Library passes false).
  More tab has no toolbar/dividers.
- One continuous background per main tab (2026-11-12): only the title
  capsule and the bottom bar may read as separate elements. Three
  things used to split it: (1) the toolbar's pinned scroll behavior
  tinted it (surfaceTint overlay) as soon as the grid scrolled, (2)
  the category tab row drew a divider line, (3) the inner Library
  Scaffold re-applied the system-bar insets HomeScreen's outer
  Scaffold already applied, so the content started a status-bar
  height below the toolbar and ended a nav-bar height above the
  bottom bar. Fixed in LibraryTab (no scroll behavior on the
  toolbar; inner Scaffold takes
  `contentWindowInsets = WindowInsets(0, 0, 0, 0)`) and LibraryTabs
  (`divider = {}`, transparent container). The same treatment now
  applies to the other four main tabs: Updates (UpdatesScreen: no
  scroll behavior on UpdatesAppBar, inner Scaffold insets zeroed),
  History (HistoryScreen: same for its SearchToolbar), Browse
  (TabbedScreen: inner Scaffold insets zeroed, PrimaryTabRow
  `divider = {}` + transparent container; its toolbar already had
  no scroll behavior), More (MoreScreen: inner Scaffold insets
  zeroed). Selection/action-mode toolbar tint is untouched
  (deliberate signal), sub-screens are untouched.

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
  The wiring (`Typefaces.kt` + `typography = Typefaces.appTypography`
  in `TachiyomiTheme.kt`) was pushed 2026-11-12 after three earlier
  attempts failed on approval timeout; the theme now actually uses
  the font everywhere.
- **Library grid:** cover radius 12.dp with a 0.75.dp outline in
  `MaterialTheme.colorScheme.outline` (`MangaCover`), so it follows
  the active Material theme (light/dark, any AppTheme) instead of a
  hardcoded grey; grid gaps 16.dp horizontal / 4.dp vertical,
  comfortable title below the cover in bodyMedium Medium (max 3
  lines), compact title on a black gradient scrim. The static glass
  gloss that used to sit on every cover (a diagonal white gradient,
  `glassEffect = true` at all 12 call sites) was removed 2026-11-12
  at the user's request and replaced by this outline; `MangaCover`
  no longer has a `glassEffect` parameter.
- **Floating bar:** selected tab now uses the app primary color
  (Neko pink/yellow) instead of onSurfaceContainer; unselected uses
  onSurfaceVariant. The bar itself is unchanged (Miuix pill, labels
  toggle kept).
- **Ripple:** primary color at 0.6 alpha for all states.
- **Not done (and why):** Neko's Feed merge (Updates+History into
  one tab) and its edge-attached NavigationBar are not copied —
  Tychii keeps its 5 tabs and floating bar. The search top bar is
  also not done. The detail page itself is Miuix now (see "Manga
  detail page" below); only its cover-palette chain was kept as
  designed. The reader is untouched.
- History: the settings and More pages were first restyled to Miuix
  on 2026-11-11 and reverted the same day at the user's request
  ("不变动其他页面"); the user reversed that the next day and the
  settings pages were migrated then — see "Settings pages" below.

### Rename: Mihon → Tychii

- `i18n/src/commonMain/moko-resources/base/strings.xml` — `app_name`
  is now `Tychii` (launcher label and app name).
- Tracker `User-Agent` headers now say `Tychii v...` instead of
  `Mihon v...` (Anilist, Bangumi, Comick, Kavita, Kitsu, Komga,
  MangaBaka, MangaUpdates, MyAnimeList, Shikimori).
- Package name, applicationId and code references to Mihon are untouched.

## Icon: Taiji (2026-11-12, day/night split 2026-11-13)

The launcher icon is a Taiji (yin-yang) symbol. Day and night use
different versions (2026-11-13, from the user's reference image —
this reverses the 2026-11-12 decision to use one icon for both
modes): day is a white background with a black-dominant Taiji and a
black outline; night is a near-black `#1B1A1F` background with the
same Taiji geometry and all colors inverted — white-dominant Taiji,
white outline. The splash background (`@color/splash`) follows:
white in day mode, `#1B1A1F` at night, so the splash looks exactly
like the launcher icon in both modes.

- Foreground (`ic_launcher_foreground.xml` day, `drawable-night/`
  night): a Taiji — black half + white half with an S-curve, a white
  dot in the black half and a black dot in the white half, black
  outline (stroke 6) in day mode. The night file keeps the same
  `pathData` and swaps every color (white half ↔ black half, both
  dots, outline white).
- Background (`ic_launcher_background.xml` white day /
  `drawable-night/` `#1B1A1F` night — the dark value is measured
  from the reference image).
- Monochrome (`ic_launcher_monochrome.xml`): the Taiji half as a
  solid shape, for themed app icons (single color, no night variant).
- The debug source set mirrors main (foreground + background, day +
  night) so Android Studio's debug build shows the Taiji too.
- Splash (`ic_mihon_splash.xml`): uses the launcher foreground
  directly, with `@color/splash` as the background, so the splash
  looks exactly like the launcher icon.

## Settings pages (2026-11-12)

All settings pages are Miuix now. Only the presentation layer changed:
ViewModels, data layer, navigation and setting keys are untouched, and
every switch / option value / `onValueChanged` gate behaves as before.
Appearance's nav-bar-labels switch and bar-style list are preserved.

- **Central renderer** (`presentation/more/settings/`):
  `PreferenceScaffold` is a Miuix `Scaffold` + `SmallTopAppBar`
  (back icon is Mihon's `MaterialSymbols.AutoMirroredRounded.ArrowBack`,
  not a Compose material icon); `PreferenceScreen` puts each
  `PreferenceGroup` under a `SmallTitle` header and inside one Miuix
  `Card` (12.dp side margins, 12.dp between groups, no dividers);
  consecutive top-level items share one card too, like a group without
  a header (2026-11-13 — they used to each get their own card, showing
  a hairline gap between every two boxes; see "Build fixes"). Search
  highlight scrolling counts three lazy items per group (header, card,
  spacer) and two per run of top-level items (card, spacer). The
  highlight blink (`highlightBackground`, M3 surfaceTint) is kept and
  applied to the Miuix rows.
- **Widgets** (`widget/`): `SwitchPreferenceWidget` /
  `TextPreferenceWidget` / `CheckboxPreferenceWidget` are thin wrappers
  over Miuix `SwitchPreference` / `ArrowPreference` /
  `CheckboxPreference` (checkbox at the end, as before); row icons go in
  the `startAction` slot as M3 icons in primary tint with 6.dp end
  padding. `TextPreferenceWidget` always draws the Miuix arrow, even on
  rows that are actions rather than navigation (e.g. About's version
  row). `ListPreferenceWidget` is an `OverlaySpinnerPreference`
  (value also stays in the subtitle, `showValue = false`); the
  `ListPreferenceDialog` used by extension source settings is an
  `OverlayDialog` with `RadioButtonPreference` rows. Multi-select uses
  an `OverlayDialog` with Miuix `Checkbox` rows and OK-only submit.
  Edit-text uses an `OverlayDialog` with a Miuix `TextField`. Sliders
  use Miuix `SliderPreference` with an Int/Float round-trip.
  `TrackingPreferenceWidget` is a Miuix `BasicComponent` row (logo,
  name, username, refresh + green check). `InfoWidget` is Miuix text.
  Theme mode is a Miuix `TabRow` (system / light / dark); the theme
  preview list keeps its Material3 preview cards (the previews render
  each theme, so M3 colors are the point). `TriStateListDialog` is an
  `OverlayDialog` with Miuix `Checkbox` in `ToggleableState`
  (indeterminate = excluded). Dialogs are the `Overlay*` variants
  throughout (the window variants can crash when the host page is
  replaced while the popup is showing); option lists are capped at
  400.dp high and scroll.
- **Main list** (`SettingsMainScreen`): Miuix `Scaffold` +
  `SmallTopAppBar` with a Miuix search button; the ten entries are
  `ArrowPreference` rows in a single grouped card. The tablet two-pane
  structure (`TwoPanelBox`, selected row, `getPalerSurface`) is
  unchanged; the per-row scroll-to-selected is gone (one card, nothing
  to scroll to).
- **About / search** (`AboutScreen`, `SettingsSearchScreen`): About
  uses a Miuix top bar and one grouped card for its rows (version,
  check for updates, what's new, licences, privacy policy). The
  `LogoHeader` icon and the Mihon social links row (website, Discord,
  X, Facebook, Reddit, GitHub) were removed 2026-11-13; `LogoHeader`
  had no other caller and was deleted. The search page keeps its
  own text field in a plain Miuix top-bar row (Miuix `SearchBar` is a
  full search UI and did not fit) and its M3 result rows.
- **Theme mode**: the 系統 / 淺色 / 深色 TabRow uses `itemSpacing =
  0.dp` (2026-11-13, from the user's Appearance screenshot: Miuix's
  default 9.dp spacing left black gaps between the three options, so
  they read as three separate pills; flush tabs form one segmented
  control). The same widget is reused by the onboarding theme step.
- **Data / Advanced / Tracking**: top-bar help buttons are Miuix
  `IconButton`s; the backup create/restore segmented buttons are two
  Miuix `Button`s; the export-column dialog, the Shizuku-missing
  dialog and the tracking login/logout dialogs are `OverlayDialog`s.
  The tracking login dialog keeps M3 text fields (username + secure
  password with visibility toggle) inside the Miuix dialog shell; the
  logout button keeps its red color via `ButtonDefaults.buttonColors`.
- **Sub-pages**: language, licences, clear-database, extension
  stores, backup create/restore, and the debug screens are unified
  too — Miuix top bars throughout; the language list uses
  `RadioButtonPreference` rows; backup option lists use Miuix
  `CheckboxPreference` rows in cards; extension-store cards are Miuix
  `Card`s and their dialogs are `OverlayDialog`s with Miuix text
  fields. Left as Material3: the licences list (third-party M3
  `LibrariesContainer`), the clear-database item list and
  `LazyColumnWithAction` bottom actions, backup warning banners,
  `StorageInfo`'s progress display, and the debug screens' monospace
  text dumps — all content, not preference rows.
- **Kept Material3**: M3 text fields where Miuix has no equivalent
  input (tracking login, extension-store URL fields — Miuix
  `TextField` has no `supportingText`/`isError`, so the message is a
  plain text line under the field), M3 `CircularProgressIndicator`s,
  and the M3 `ScrollbarLazyColumn` / `LazyColumnWithAction` containers.
  Nothing else on the settings pages is Material3.

## Manga detail page (2026-11-12)

The manga detail page is Miuix now. Only the presentation layer
changed: ViewModels, data layer, navigation and all click behaviour
are untouched, and the tablet two-pane layout keeps its structure.

- **Toolbar** (`MangaToolbar`): Miuix `SmallTopAppBar` with Miuix
  `IconButton`s. The scroll fade is kept — the bar background and the
  title fade in through the `color` / `titleColor` alpha (the small
  top bar takes plain colors, so there is no title-alpha modifier).
  Action mode shows the counter as the title with a close icon. The
  download and overflow menus are `OverlayDropdownPopup`s with the
  same six download actions / overflow entries as before.
- **Info header** (`MangaInfoHeader`): the action row and the
  description + tags sit in Miuix `Card`s (12.dp side margins); tags
  are small Miuix `Button`s (Miuix has no chip) and the tag
  search/copy menu is an `OverlayDropdownPopup`. The description
  collapse scrim now fades into the card color instead of the page
  background. The cover palette chain is untouched: backdrop
  `allowHardware(false)`, 0.25-alpha wash, action-row accent,
  `rememberThemeColor` / `quietVibrant`, silent fallback.
- **Chapter list**: rows keep `SwipeableActionsBox` (Miuix has no
  swipe actions) and their theme colors; rows are separated by Miuix
  `HorizontalDivider`s with 16.dp side insets (no divider after the
  last chapter). The divider carries 8.dp vertical padding on both
  sides (2026-11-13, from the user's detail-page screenshot: it used
  to sit flush under the date line, reading as part of the chapter
  above; it now sits centered between two chapters, 20.dp each way).
  The chapter header is a Miuix subtitle-style section
  title; the missing-chapters row uses a Miuix divider. The download
  indicator's menus are `OverlayDropdownPopup`s.
- **Assembly** (`MangaScreen`): Miuix `Scaffold` with the M3
  `SnackbarHost`, the M3 extended start/resume FAB and the M3
  `PullRefresh` inside it. The page background stays the app theme
  background, so the backdrop gradient and palette wash still match.
  The FAB height is measured and added to the list bottom padding,
  as the Material Scaffold wrapper did before.
- **Bottom action menu** (`MangaBottomActionMenu`): container color
  is now Miuix `surfaceContainerHigh`. The buttons stay custom
  (long-press reveals the label; Miuix `IconButton` has no
  long-click). `LibraryBottomActionMenu` in the same file is
  untouched — it belongs to the library page.
- **Dialogs**: delete-chapters and set-interval are `OverlayDialog`s;
  the interval wheel is a Miuix `NumberPicker` (0 = default label).
  The chapter settings dialog rebuilds the tabbed sheet locally with
  Miuix `TabRow` + `OverlayBottomSheet` (the shared `TabbedDialog`
  stays Material3 — the library, reader and updates dialogs use it);
  its rows are `BasicComponent` + `Checkbox` (tri-state),
  `RadioButtonPreference` (display mode) and a `BasicComponent` with
  an up/down arrow (sort). The scanlator filter dialog is an
  `OverlayDialog` with Miuix `Checkbox` rows. The duplicate-manga
  sheet is an `OverlayBottomSheet` with Miuix `Card` items and a
  Miuix `TextButton` for cancel. Dialog OK buttons use the primary
  `TextButton` colors, cancel the default ones.
- **Cover dialog** (`MangaCoverDialog`): action-pill buttons are
  Miuix `IconButton`s and the pill is Miuix `surfaceContainer`; the
  edit menu stays a Material3 `DropdownMenu` (the dialog is a
  platform window with no Miuix popup host) and the image viewer is
  untouched.
- **Notes**: the notes page is a Miuix `Scaffold` +
  `SmallTopAppBar` (title + manga subtitle); the notes section's edit
  button is a Miuix `Button` and its divider is Miuix. The rich-text
  editor itself is untouched (no Miuix equivalent).
- **Kept Material3**: the action-row buttons (they need long-press —
  Miuix `Button` has none), the bottom-menu buttons (same reason),
  the swipe box, `PullRefresh` (Miuix's has no `enabled` flag, and
  pull-to-refresh must stay off while selecting), the extended FAB
  (Miuix has no extended FAB), the `SnackbarHost` (the host state is
  Material3), the fast scroller, the markdown renderer and the
  rich-text editor / notes display.

## Browse pages, migration and shared components (2026-11-12)

Browse sub-pages, migration flows and the shared top bar / dialogs are
Miuix now. Only the presentation layer changed: ViewModels, data layer,
navigation and all click / drag behaviour are untouched.

- **Shared top bar** (`presentation/components/AppBar.kt`): the `AppBar`
  and `SearchToolbar` keep their exact signatures (40 call sites, so
  every remaining page gets the new bar for free), but render a Miuix
  bar — Miuix surface background, Miuix `IconButton`s, and the overflow
  menu is an `OverlayDropdownPopup`. Miuix's `SmallTopAppBar` only
  takes a plain String title, while callers pass composable titles
  (search field, count pill, marquee subtitle), so the bar is a small
  custom layout in Miuix colors rather than `SmallTopAppBar` itself.
  `scrollBehavior` is kept for call-site compatibility but ignored:
  the Miuix bar stays pinned, so the scroll tint it used to drive is
  gone. Tooltips stay Material3 (Miuix has none).
- **Tabbed dialogs** (`TabbedDialog.kt`): the tab row is Miuix `TabRow`
  and the overflow icon is a Miuix `IconButton`; the sheet container
  is still `AdaptiveSheet`. No caller uses `tabOverflowMenuContent`,
  so its menu content stays the Material3 dropdown wrapper.
- **Adaptive sheet** (`presentation-core/.../AdaptiveSheet.kt`): the
  sheet surface color is now Miuix `surfaceContainerHigh` (both the
  tablet-centered and phone bottom-anchored branches). The custom
  AnchoredDraggable behaviour, swipe-to-dismiss and 460.dp max width
  are unchanged — Miuix's `OverlayBottomSheet` is always
  bottom-anchored and cannot reproduce the tablet-centered dialog,
  and the sheet has no visible drag handle (none before either).
- **Browse rows** (`BaseBrowseItem`, `BaseSourceItem`): rows keep
  their slots and 48.dp source/extension icons, with Miuix row
  spacing (16.dp horizontal, 12.dp vertical). Section headers in
  Sources / Extensions / Migration config are Miuix `SmallTitle`s.
- **Manga list item** (`CommonMangaItem.MangaListItem`, used by the
  library list view, browse-source list and global search): the row
  keeps its layout and selection background, gains a Miuix
  `HorizontalDivider` with a 16.dp start inset in the browse-source
  list (the library list view passes `showDivider = false` since the
  2026-11-12 seam unification), and the continue-reading button is
  a Miuix `IconButton` with the same primary-container fill. The Neko
  grid items are untouched. `BaseMangaListItem` (migration only) gets
  the same divider.
- **Dialogs**: `SourceOptionsDialog`, `RemoveMangaDialog`, the two
  extension dialogs, the migration dialog, the extension-details
  content-warning dialog and the browse-source display-mode menu are
  `OverlayDialog` / `OverlayDropdownPopup` with Miuix `TextButton`s.
  `LabeledCheckbox` (shared core component, used by the migration,
  history, delete-library-manga and track dialogs) now draws a Miuix
  `Checkbox`. The source-preferences edit-text dialog deliberately
  stays Material3 `AlertDialog`: it must be a platform dialog window
  so extensions can find `android.R.id.button1` in its view hierarchy
  and so the keyboard can be forced visible through the dialog
  window; a comment in the file says so.
- **Extensions**: update-all is a Miuix `Button`; the three action
  icons are Miuix `IconButton`s; the incognito and per-source switches
  are Miuix `Switch`es; dividers are Miuix. The permission warning
  banner, the install progress ring and the extension pills stay as
  they were (Miuix has no banner or small pill).
- **Global search**: the filter chips are small Miuix `Button`s
  (Miuix has no chip; selected uses the primary button colors, like
  the tags on the manga detail page), the progress bar and dividers
  are Miuix, the result rows use a Miuix `IconButton` and the loading
  spinner is Miuix. The card row itself is the unchanged Neko grid.
- **Migration**: the config screen keeps its reorderable list and
  drag handles; the grouped rows are flat Miuix `surfaceContainer`
  with the same first/last corner shape (Miuix `Card` takes one
  uniform corner radius, so it cannot do top-only / bottom-only
  rounding), the per-source rows are plain Miuix-spaced rows, the
  flag chips are Miuix `Button`s, the switch rows are Miuix
  `SwitchPreference`s, and the sheet's start button is a Miuix
  primary `Button`. The extended FABs on the migration screens stay
  Material3 (Miuix has no extended FAB, same as the detail page).
  The sheet's extra-search-query field stays `OutlinedTextField`:
  Miuix `TextField` has no `supportingText`, and the hint text under
  the field is part of the UI.
- **Empty states**: browse-source, missing-source, extension-filter,
  sources-filter and migrate-manga empty/error states now use the
  quiet `QuietEmptyScreen` instead of the shared `EmptyScreen`.

## Remaining pages: updates, history, download, category, stats, upcoming, misc (2026-11-12)

The second half of the browse batch: the main-tab list items and
sub-pages, plus the track dialogs and the WebView page. Only the
presentation layer changed: ViewModels, data layer, navigation and
all click / drag behaviour are untouched.

Shared core (one change, many callers):

- `ListGroupHeader` is now a Miuix `SmallTitle` (16.dp horizontal /
  8.dp vertical inside margin). This covers the date group headers
  in Updates, History and the download queue in one place.
- `TriStateItem` (in `SettingsItems.kt`) is now a Miuix basic
  `Checkbox` with `ToggleableState` (Off / On / Indeterminate =
  excluded), the same pattern as the settings `TriStateListDialog`.
  Used by the updates filter dialog.
- `SectionCard` is now a Miuix `Card` (12.dp horizontal / 4.dp
  vertical margins, 16.dp inside margin) with a `SmallTitle`
  header. Its only callers are the stats sections.
- `InfoScreen` is now a Miuix `Scaffold` with a Miuix primary
  `Button` for accept and a Miuix `Button` for reject, and the
  divider colour is Miuix `dividerLine`. Onboarding and the crash
  screen call `InfoScreen` directly, so they needed no edit of
  their own.

Updates / history:

- Chapter rows (`UpdatesUiItem`) and history rows (`HistoryItem`)
  are Miuix now (dividers under them were removed in the 2026-11-12
  seam unification); action icons in history rows are Miuix
  `IconButton`s.
- The updates filter dialog uses Miuix `Switch` / dividers (its
  rows are the now-Miuix `TriStateItem`); the delete-confirmation
  dialog and both history delete dialogs are `OverlayDialog`s
  with Miuix `TextButton`s. `UpdatesScreen` / `HistoryScreen`
  themselves needed no edit (shared `AppBar` / `SearchToolbar` /
  `ListGroupHeader` / `QuietEmptyScreen` already).

Download queue:

- Progress is a Miuix `LinearProgressIndicator` (Float progress,
  not a lambda); the item menu is an `OverlayDropdownPopup`; the
  dragged row colour is Miuix `surfaceContainerHigh`. Reorder
  and drag-handle logic are untouched.
- The sort menu in `DownloadQueueScreen` (a `DropdownMenu` with
  two nested submenus via `NestedMenuItem`) is now a single
  `OverlayDropdownPopup` using `DropdownItem(children = ...)`
  cascading items. `NestedMenuItem` now has zero callers.

Category:

- The empty state is `QuietEmptyScreen`; category rows use a
  Miuix `Card(onClick = onRename)` with Miuix `IconButton`s, drag
  handle kept.
- All four category dialogs are `OverlayDialog`s. Create / Rename
  keep the M3 `OutlinedTextField` inside: Miuix `TextField` has
  no `supportingText` / `isError`, and the duplicate-name message
  is part of the UI. Delete and both `ChangeCategoryDialog`
  branches use Miuix `TextButton`s; checkboxes are Miuix
  `Checkbox` (`ToggleableState`, via `asToggleableState()` for
  tri-state).

Stats / upcoming:

- Stats needed no direct edit beyond `SectionCard` (top bar is
  the shared `AppBar`).
- Upcoming date headings are `SmallTitle` rows with a Miuix
  `Badge` for the count; upcoming items have the Miuix divider;
  calendar prev/next are Miuix `IconButton`s; the divider in
  `UpcomingScreen` is Miuix. The calendar day grid itself
  (`Calendar.kt` / `CalendarDay.kt` / `CalendarIndicator.kt`) is
  a custom layout with no Miuix equivalent and was not changed.

Misc sub-pages:

- `NewUpdateScreen`: the skip dialog is an `OverlayDialog`, the
  reject-action `OutlinedButton` is a Miuix `IconButton`, the
  open-in-browser `TextButton` is a Miuix `Button`.
- `SupportUsScreen`: M3 `Scaffold` / `Card` are now Miuix
  `Scaffold` / `Card` (rows already used Miuix preference
  widgets).
- `WebViewScreenContent`: the two loading progress bars are
  Miuix `LinearProgressIndicator`s. The `WebView` itself, JS
  dialogs and navigation logic are untouched.

Track dialogs:

- `TrackInfoDialogHome`: the private badge is a Miuix `Badge` /
  `BadgedBox`, dividers and the overflow `IconButton` are Miuix,
  the item menu is an `OverlayDropdownPopup`, the add-tracking
  button is a Miuix `TextButton`.
- `TrackerSearch`: the top bar is the shared Miuix-style `AppBar`
  (title slot holds the search field), the bottom confirm buttons
  are Miuix `Button`s, the result card uses Miuix
  `surfaceContainer` with a Miuix primary border when selected,
  and its long-press menu is an `OverlayDropdownPopup`.
- `TrackInfoDialogSelector`: radio buttons and dialog buttons
  are Miuix (`RadioButton`, `TextButton`, `HorizontalDivider`).
  The `DatePicker` stays Material3 (Miuix has no date picker),
  and the selectors still render inside the M3
  `AlertDialogContent` host in `TrackInfoDialog.kt` (manga
  detail area, out of scope for this batch).

Deliberately kept Material3 in this batch:

- Extended FABs (`CategoryFloatingActionButton`,
  `MigrateMangaScreen`): Miuix has no extended FAB.
- The calendar day grid (see above) and the track `DatePicker`.
- Text fields with a hint / error message under them
  (migration sheet extra-search-query field, category
  create/rename dialogs): Miuix `TextField` has no
  `supportingText` / `isError`.
- The `TrackInfoDialog` host dialog (`AlertDialogContent` in
  `ui/manga/track/TrackInfoDialog.kt`): out of scope, belongs
  to the manga detail area.
- Tooltips everywhere (Miuix has none).

## Deliberately NOT changed

- The reader: gesture-driven fullscreen viewer, highest risk.
- Neko's Feed merge (Updates+History into one tab) and its
  edge-attached NavigationBar: Tychii keeps its 5 tabs and floating
  bar.
- `miuix-nav` was not adopted; Mihon's existing navigation stays.

## Build fixes

Local build errors reported by the user after each push, fixed the
same way (read the file, check the Miuix/library source before
asserting an API is unavailable, scan tree-wide for the same error
class, push, verify HEAD). Compilation is unverified in the cloud
(the Gradle daemon fails to start there); the user builds locally
from the GitHub ZIP.

- 2026-11-12 — `MangaViewModel` was missing the
  `kotlinx.coroutines.flow.asStateFlow` import, so the build failed
  with "Unresolved reference 'asStateFlow'" at the `vibrantColor`
  declaration. One import line (c09f6fb8).
- 2026-11-12 — the 0.1.2 migration left
  `SettingsItems.CheckboxItem` calling Miuix `Checkbox` with
  Material3's `checked`/`onCheckedChange`; Miuix `Checkbox` takes
  `state: ToggleableState` + `onClick`. Fixed by passing
  `ToggleableState.On/Off` with `onClick = null` (the row handles
  the click); every other Miuix `Checkbox` call site already used
  `state` (7930bd93).
- 2026-11-12 — two missing imports from the same migration:
  `CategoryDialogs.kt` used `Arrangement.End` with no
  `layout.Arrangement` import, and the detail page `MangaScreen.kt`
  used `0.dp` / `16.dp` with no `unit.dp` import. A tree-wide scan
  for missing `dp` / `sp` / `ToggleableState` / `Arrangement` /
  `asStateFlow` imports then came back clean (907b7131).
- 2026-11-12 — seven more errors from the same migration:
  `CategoryDialogs` closed the dialog content lambda before the
  buttons row (stray `)`); `SettingsTrackingScreen` had a duplicated
  `}` that closed the enclosing object early; `ChapterSettingsDialog`'s
  `cycle` lambda inferred `() -> Unit?` and MultiSelect's `toggle`
  inferred `() -> Boolean` (both now explicit `() -> Unit`);
  `MangaDialogs` called `stringResource` inside NumberPicker's plain
  `(Int) -> String` label (string now hoisted); `TrackerSearch` used
  `onSurfaceVariant`, which Miuix lacks (now `onSurfaceVariantActions`,
  per Miuix's Monet mapping) (7b3eb981).
- 2026-11-12 — seam unification: the top toolbar band was pure black
  (AppBar's Miuix `surface` default) while the page is the theme
  background, and list-item dividers drew grey ruled lines. The four
  tab toolbars now pass `MaterialTheme.colorScheme.background`, and
  the divider is gone on the Library/Updates/History tabs (01049272).
- 2026-11-12 — Browse tabs as Miuix capsules, like the Library
  category tabs (2efd108a).
- 2026-11-13 — settings groups merged: top-level preferences that
  are not in a group used to each get their own Card, showing a
  hairline gap between every two boxes while grouped items sat in
  one continuous card (seen on the Downloads page). PreferenceScreen
  now collects consecutive top-level items into one shared Card,
  like a group without a header; `findHighlightedIndex` was updated
  to the new lazy-item layout (e001cc46).

## Build

JDK 21, Android SDK API 37, then:

```sh
./gradlew :app:assembleDebug
```

Not verified in this environment: the local Gradle daemon fails to start
here (dispatch/Broken pipe before compilation), the same failure seen
with the earlier cloud build. Build on a local machine to verify.
