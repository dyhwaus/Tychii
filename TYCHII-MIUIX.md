# Tychii × Miuix — UI refactor notes

Tychii is a fork of Mihon, restyled with the Miuix component library
(HyperOS visual language, `top.yukonga.miuix.kmp`, v0.9.4) and renamed
from Mihon. This note records what changed, what was deliberately not
changed, and how to build.

Current version: **v0.2.0** (versionCode 42) — every user-visible page
follows the Miuix style, including the reader's floating bars (same
liquid glass as the bottom bar). The reader viewer itself is untouched
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

## Icon: frosted disc + 極 (2026-11-13)

Redesigned 2026-11-13 to imitate Mihon's original icon: a circle with
the 極 character in the middle, in a frosted-glass skeuomorphic style.
This supersedes the Taiji icon (2026-11-12–13). The disc keeps Mihon's
background colour (`#FAFAFA`) but is larger than Mihon's (outer circle
r 120, ring to r 97.4; was r 103.5 / r 84). The ring is black by day
and white at night (flat `#000000` / `#FFFFFF`; it was Mihon's
`#0058A0` blue until 2026-11-13). Day and night are separate versions
(day/night split kept from the Taiji change). The 極 started as Mihon's
original glyph path, then a
proper traditional 極 from M PLUS Rounded 1c Bold (the app's own
font, SIL OFL). Later the same day the user sent their own font file
(`GangTieSongTiSpecial-1_1_dwmp.ttf`, "FAKE-MTRSong 80's Special V4",
an MTR Song style replica — not the commercial MetroSung) and the 極
was replaced with that font's glyph, converted from its TrueType
outline (quadratic → cubic, `fillType="evenOdd"` so the enclosed
counters stay holes; this glyph is several disjoint contours, not one
outer loop). The glyph is about 130 units wide, centred on the disc.
The font file itself is not in the repo — only the single 極 outline
is used, in the launcher icon.

- Day foreground: frosted light disc (white → pale blue vertical
  gradient), top sheen + bottom shade clipped to the disc, flat black
  ring with a thin white rim-light arc on top, dark `#031019` 極
  with a soft drop shadow.
- Night foreground: frosted dark disc (dark blue-grey gradient),
  flat white ring (no rim light — white on white would be invisible),
  near-white `#EDF4FA` 極, background `#1B1A1F`.
- The frosted effect is faked with linear gradients only (a vector
  can't do noise): disc gradient, sheen/shade caps, ring gradient,
  rim light. Gradients use `aapt:attr` (`xmlns:aapt`), API 24+;
  minSdk is 26.
- Monochrome (`ic_launcher_monochrome.xml`): unchanged, Mihon's 極
  as a solid shape for themed app icons.
- The debug source set mirrors main (foreground + background, day +
  night) so Android Studio's debug build shows the new icon too.
- Splash (`ic_mihon_splash.xml`): uses the launcher foreground
  directly, with `@color/splash` as the background (`#FAFAFA` day,
  `#1B1A1F` night), so the splash looks exactly like the launcher
  icon.

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

## Explore page: per-source browsing (2026-11-13)

The per-source page (tap a source in Browse to see its manga, the
"explore" page) still had the last Material3 chips in the app: the
listing selector (Popular / Latest / Filter) was a Material3
`FilterChip` row. It now uses the same small Miuix `Button` chips as
the global search toolbar (selected = primary button colors), as a
private `FilterButton` in `BrowseSourceScreen.kt`. The top band
(toolbar + chips) sits on Miuix `surface` instead of Material
surface, so there is no seam between the toolbar and the chips row,
and the divider under the chips is Miuix. The filter sheet
(`SourceFilterDialog`) keeps its shared filter rows (checkbox and
tri-state are already Miuix; text / select / sort rows are shared
with settings and stay as they are), but its sticky header is Miuix
now: a `TextButton` for Reset, a primary Miuix `Button` for Filter,
a Miuix divider, and the header background matches the sheet surface
(`surfaceContainerHigh`).

## Hide the Updates tab (2026-11-13)

A new Appearance setting, "隐藏更新页" (`pref_hide_updates_tab`,
`UiPreferences.hideUpdatesTab`, default off). When on, the Updates
tab is removed from the bottom bar (and the tablet navigation rail):
`HomeScreen` builds its tab list as `TABS - UpdatesTab` and both
the phone bar and the rail render that list, so a hidden tab is
never selectable. Two guards keep the navigator consistent: if the
Updates tab is the current tab when it is hidden, it falls back to
the Library tab, and the Updates launcher shortcut (`Tab.Updates`
from `MainActivity`) opens the Library tab instead — the shortcut
handler reads the preference directly so the value is never stale.
The change takes effect immediately, no restart needed. The updates
themselves are untouched (they still arrive; only the tab is hidden).

## Browse tab: first-level menu like the More page (2026-11-13)

The Browse tab is no longer a pager of three tabs (来源 / 拓展套件 /
迁移）. It is a first-level menu like the More page: one grouped
Miuix card with three `ArrowPreference` rows (来源， 拓展套件， 迁移）,
and tapping a row opens that section as its own screen
(`BrowseSectionScreen`, one per section). Each section is still the
same `TabContent` the pager used, rendered with its own top bar
(back button, title, the section's actions) instead of the shared
tab bar; the Extensions section keeps its search field (the title
capsule hides while it is open, as before). The launcher shortcuts
still open their section directly: the Sources and Extensions
shortcuts send a `BrowseSection` through a buffered channel that the
menu collects and pushes. Tapping the Browse tab again still opens
global search. The `TabbedScreen` composable is now unused (Browse
was its only caller); the `TabContent` type still lives in that file
and is used by the section wrappers. Each section creates its own
ViewModels on entry now (they used to live on the Browse tab and
survive tab switches); the lists are reactive Flows, so entry is
just a re-subscription.

## Library view options as small icons (2026-11-13)

The Library page's sort / display-mode / filter options are back as
small icons. They used to live in the top bar, but the centered
title capsule sits over that band on phones, so they moved to the
end of the category tab row (`LibraryViewOptions`, pinned right;
when there are no tabs the icons stand alone on the same row):
- Sort (sort icon) opens a Miuix dropdown with the same sort list
  as the settings sheet (tracker score only when a tracker is
  logged in). The selected row carries an up/down arrow for the
  direction, and tapping the active type flips it — the same rule
  as the sheet.
- Display mode (icon shows the current mode: grid / comfortable
  grid / list) opens a dropdown with the four modes.
- Filter opens the settings sheet; its icon takes the accent tint
  while filters are active.
The settings sheet itself is unchanged (still the full sort /
display / filter pages). The top-bar actions stay tablet-only.

## Bottom-bar clearance lives on the lists (2026-11-13)

The tab content used to end 96.dp above the bottom (a padding on
HomeScreen's outer content box), leaving a dead band behind the
floating bar — and the covers still scrolled under it, so the last
row showed only half a cover at the bottom. The band is gone: the
content now runs to the system navigation bar and the page is one
continuous background (the Scaffold's). Each tab list pads its own
bottom instead, by the shared `LiquidNavBarClearance` (96.dp, in
`TychiiLiquidBar.kt`): Library grid/list, Updates, History, Browse
menu, More. The last item now scrolls fully above the bar. (The
bar is hidden while the Library selection menu is showing, so the
extra bottom space there is harmless.)

The top got the same treatment (2026-11-13) and then it was
reverted the same day: the first row was being cut off by the title
capsule band, so each list added a shared 32.dp gap above its first
item — but the gap read as a strange blank space above the covers
and did not fix the cut-off, so it was removed again. The 64.dp
toolbar band the Scaffold already provides leaves enough room on
its own. The Library had an extra bug here: it built a fresh
`PaddingValues` with only the bottom, dropping the Scaffold's own
top padding (the 64.dp band), so its first row slid under the band;
it now adds to the Scaffold's padding with the `plus` operator like
the other lists.

The cut-off turned out to be the covers themselves, not the band:
the grid cells came out square in the lazy grid even though the
cover box asked for `aspectRatio(2/3)`, so portrait covers were
cropped top and bottom by `ContentScale.Crop`. `MangaGridCover`
now sizes its box from its measured width (`BoxWithConstraints`,
height = width / 2/3), so the cells are true 2:3 and the covers
show in full.

One more top-spacing fix (2026-11-13): the Library counted the
64.dp toolbar band twice — its root Column pads the top by the
Scaffold's padding, and the grid added the same padding again — so
its first row sat much lower than the other pages' first item. The
grid now takes only the bottom clearance (plus the Scaffold's
bottom, for the selection menu); the Column already places it below
the band. History, Updates, Browse and More were already at the
band (64.dp): their lists are direct Scaffold content, so they add
the Scaffold's padding once.

## Floating action capsule (2026-11-13)

The search/sort/display/filter icons (and each tab's own actions)
live in a floating Miuix capsule on the right edge, over the list —
`MiuixFloatingCapsule`, same material as the title capsule and the
bottom bar (solid surfaceContainer, 0.75 alpha, or liquid glass with
blur + vibrancy + tilt highlight, by BarStyle). No layer behind it:
it floats over the covers with only a 12.dp gap from the screen
edge, covering part of the content while shown. It hides once the
user has scrolled down past 120.dp (a nested-scroll observer in
HomeScreen that consumes nothing) and comes back on any upward
scroll, on tab switch, and it hides while searching or selecting
(same condition as the title capsule). Each tab reports its actions
to HomeScreen (conflated channel, re-reported when the state they
depend on changes); a tab with no actions reports an empty list.
Library reports search/sort/display/filter (the sort/display
dropdowns still render in the Library content, from the root
scaffold; the tab row holds only the category tabs now, and the
toolbar magnifier is hidden on phones via SearchToolbar's
`showSearchIcon` — the reset button while searching still shows).
Updates reports filter/upcoming/refresh, History reports
search/clear-history. Browse menu and More report nothing (the
capsule stays hidden there); the pushed Browse sections keep their
own toolbars. Tablets never show it (they keep the toolbar actions
and the navigation rail). Like the other two glass pieces, the
capsule must stay outside the layer recorded by `layerBackdrop`
(HomeScreen renders it as an overlay sibling of the Scaffold), or
it would blur itself and crash hwui.

## Library grid: PiliPlus-style cards (2026-11-13)

The compact grid item is now a PiliPlus-style card, after
[bggRGjQaUbCoE/PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus)
(`VideoCardV`): the whole item is a surface card (Miuix
`surfaceContainer`, 12.dp corners) with the cover on top and the
title below it (2 lines, 6.dp / 5.dp padding), instead of the title
overlaid on the cover with a black gradient scrim — which hid the
bottom of the cover. The cover keeps only its top corners rounded
so it flows into the title area (cover-only, with no title below,
keeps the full rounding). The cover badges (unread / downloads /
language) and the continue-reading button stay on the cover. This
applies to the Library compact grid and to the Browse source compact
grid, which share `MangaCompactGridItem`; the comfortable grid was
already title-below. The Miuix floating bar, title capsule and
Neko theme are untouched — only the card layout follows PiliPlus.

## Cover cards: card background removed, then reverted (2026-11-14)

The compact-grid manga card (`MangaCompactGridItem`) wrapped the cover
and title in a `surfaceContainer` card with 12.dp rounding. The
background was briefly removed (commit 6ebee96a) so the cover floated
on the page background, but the user asked to revert it the same day —
the card background is back (top-only cover rounding, full 12.dp when
cover-only), i.e. the state before 6ebee96a.

## Capsule dropdowns: opening one closes the other (2026-11-14)

The Library capsule's sort and display-mode dropdowns were two
independent booleans, so tapping Display while Sort was open left
both dropdowns up. The capsule actions now close the other dropdown
when one opens (Sort closes Display, Display closes Sort), and
opening search closes both. The Explore page had the same shape of
bug with its single display dropdown: opening search while it was up
left it open, so the Search action now closes it first. Checked the
other pages: Updates (filter + Upcoming navigation + refresh) and
History (search hides the capsule, clear-history is a dialog) have
no two popups that can be open at once, so they needed no change.
The Library capsule's filter is now a third dropdown (see below),
and the same mutual-exclusion covers it.

## Filter: capsule dropdown instead of the bottom sheet (2026-11-14)

The Library and Updates capsule Filter buttons opened Mihon's
bottom-sheet settings dialogs (LibrarySettingsDialog's Filter tab,
UpdatesFilterDialog's Filter/Categories tabs). They now expand from
the capsule as dropdowns, like Sort and Display. The Library
dropdown holds the tri-state filters (downloaded, unread, started,
bookmarked, completed, tracker); the Updates dropdown holds its
four tri-state filters, the excluded-scanlators switch, and one row
per category. Tapping a row cycles any -> only -> exclude; "only"
shows as the selected row (checkmark), "exclude" as the block icon,
any as plain. The dropdowns stay open so several filters can be set
in one go, and close by tapping outside. The Updates filter dialog
is deleted — the capsule was its only entry point. The Library
settings dialog stays: tablets have no capsule (the toolbar's
Filter button opens it there) and re-tapping the Library tab opens
it, so the display settings (columns, badges) remain reachable that
way.

## Explore capsule hidden by stale scroll state (2026-11-14)

The Explore page's floating capsule (search + cover layout) was not
showing even though the page reported its actions correctly. Root
cause: the capsule's scroll-hide state (`capsuleScrollVisible`) lives
in HomeScreen and only reset on a *tab* switch. Opening a source
means scrolling down the Sources list to find it, which hid the
capsule — and pushing the Explore page does not change the tab, so
the state never reset, leaving the capsule hidden on the new page
even though that page starts at the top. Fix (HomeScreen.kt): when
the collector receives a fresh non-empty actions report (i.e. a
screen just became current or re-reported), it resets the
scroll-hide state to visible. A screen that reports no actions still
hides the capsule, and scrolling down still hides it again.

## Reader settings: Miuix card grouping for hierarchy (2026-11-14)

The reader settings were a flat list — section headings, then chips /
checkboxes / sliders with no visual container, so there was no sense
of layers. Now the sections are grouped into Miuix `Card`s, exactly
like the main settings (`PreferenceScreen`): each section's items sit
in one card (`surfaceContainer` on the sheet's `surfaceContainerHigh`),
sections are separated by a 12.dp gap, and the section headings stay
above their card. New shared helper `ReaderSettingsCard`
(`presentation/reader/settings/ReaderSettingsCard.kt`) + a
`ReaderSettingsSectionGap()`.

- `ReadingModePage`: per-series card (reading mode + orientation
  chips); the pager and webtoon viewer settings each get their own
  card (only one shows at a time).
- `GeneralSettingsPage`: three cards — theme + show page number;
  vertical navigator (chips + the conditional left-side checkbox and
  height slider); the general checkboxes incl. the flash-page
  controls (sliders + colour chips appear inside the card when
  flash is on).
- `ColorFilterPage`: three cards — custom brightness; colour filter
  (RGBA sliders + mode chips when on); grayscale + inverted.
- `WebGpuViewerSettings` (the WebGPU viewer path): its 9 remaining
  Material3 `FilterChip`s are now `ReaderFilterChip` too, so no bare
  `FilterChip` is left in the reader settings. Its shader lists and
  reorder UI are deliberately left alone — a specialised screen, not
  a plain settings list.

## Chip rows: vertical gap between wrapped rows (2026-11-14)

When a `SettingsChipRow` wrapped to two rows (e.g. the reader's
screen-orientation options), the rows sat directly on each other
with no gap — the shared `FlowRow` only had a horizontal
`Arrangement.spacedBy`, no `verticalArrangement`. It now uses the
same spacing vertically as horizontally, so wrapped chip rows
breathe. This is the shared component, so every chip row benefits
(reader settings, library settings, web-GPU viewer settings).

## TabbedDialog TabRow seam (2026-11-14)

The reader settings dialog's three tabs (reading mode / general /
filter) showed the same seam as the theme-mode TabRow: the
TabRow's default row background is Miuix `surface` (#F7F7F7 in
light mode, measured 247 in the user's screenshot), but the sheet
it sits on is `surfaceContainerHigh` (#E8E8E8, measured 232), so a
mismatched band showed behind the tab pills. Fixed in
`TabbedDialog` itself (it always renders inside an
`AdaptiveSheet`, so this fixes the same seam in every dialog that
uses it — library settings, updates filter, upcoming): the TabRow
now takes `surfaceContainerHigh` as its row background. The
selected pill is one shade up from the sheet — white
(`surfaceContainer`) in light mode, `surfaceContainerHighest` in
dark mode, where `surfaceContainer` equals the sheet and would
vanish into it. The selected pill in the screenshot was already
white, so its light-mode look is unchanged.

## Reader chrome to Miuix (2026-11-14)

First pass on the reader, scoped to its chrome — the page viewer,
gestures, tap zones and fullscreen behaviour are untouched. The
top and bottom bars' background went from Material3
`surfaceColorAtElevation(3.dp)` to Miuix `surfaceContainer`, same
translucency (0.9 alpha dark / 0.95 light) so the manga page still
shows through slightly (`ReaderAppBars`, and the chapter
navigator's pill and prev/next buttons follow the same colour in
`ChapterNavigator`). The bottom bar's four action icons are now
Miuix `IconButton`s. The reader settings dialog's chip rows (theme,
reading mode, orientation, image scale, zoom start, tap zones and
their invert mode, flash colour, colour-filter mode) are now Miuix
buttons via a shared `ReaderFilterChip` (primary when selected),
mirroring the Explore page's `FilterButton`. The settings dialog
itself (`TabbedDialog`) was already Miuix. Deliberately left alone
for now: the page indicator pill, the page-actions dialog, and the
reading-mode / orientation select dialogs (Material3
`AlertDialog` / `Surface` / `FilledTonalButton`), and the
`Slider`/`VerticalSlider` in the chapter navigator.

## Package name is com.tychii.dev (2026-11-14)

The application id changed from `app.mihon` to `com.tychii.dev`,
so the app installs as its own app and no longer collides with
Mihon. The code packages stay `eu.kanade.tachiyomi` (renaming them
would touch every file; the id is the app's identity, the code
package is just organisation). Changes: `applicationId` in
`app/build.gradle.kts`; the debug build type lost its `.dev`
suffix (the base id already ends in `.dev`, so keeping it would
have made debug `com.tychii.dev.dev`); the Comick deep-link scheme
in the manifest and its `REDIRECT_URI` in `ComickApi.kt` (they must
match each other); the two `package_name` entries in
`google-services.json`. Deliberately untouched: the `mihon://`
deep-link schemes (not the package name), and `TelemetryConfig`'s
`MIHON_PACKAGES` — that set gates Mihon's own crash reporting to
official builds, and Tychii is not one, so telemetry correctly
stays off. Telemetry is off by default in local builds anyway
(`include-telemetry` flag unset), so `google-services.json` is not
read at build time; the Firebase project there is still Mihon's,
and would need its own project for telemetry to work.

## Navigation-bar area transparent (2026-11-14)

The strip at the very bottom of the screen, around the gesture
bar, was a solid band of the app background: the HomeScreen
MiuixScaffold used the default `contentWindowInsets`
(`systemBars` + `displayCutout`), so its content was padded up
by the bottom navigation-bar inset and the covers never drew
there. The Scaffold now takes `systemBars` limited to the top
and horizontal sides, unioned with `displayCutout` — the content
extends to the very bottom edge, behind the gesture bar, and the
covers show through while scrolling. The floating bar keeps its
own `navigationBarsPadding`, and each tab list still pads its
bottom by `LiquidNavBarClearance`, so the last item still clears
the bar.

## Explore: floating action capsule like the Library page (2026-11-14)

The per-source Explore page now has the same floating action
capsule as the Library page, on the right edge: search and cover
layout. Search opens the search field in the top bar (it takes
the top bar's place while a query is set, like the Library page's
toolbar does when searching); the layout button opens a dropdown
with the four layouts (compact / comfortable / cover-only / list),
its icon following the current layout. The capsule hides while
searching. This needed one HomeScreen change: the action capsule
no longer follows the title capsule's visibility — a tab hides
the action capsule while searching or selecting by reporting no
actions then, and a page may hide the title capsule for its own
reason (Explore shows the source name as its title) while still
offering actions.

## Explore: source name header above the chips (2026-11-14)

The per-source Explore page's top bar now holds the source name
at the top left (titleLarge, bold, 40.dp of clearance from the
status bar — 28.dp was still too tight), with the filter chips
below it. The floating title
capsule is hidden on this page, since the source name is the
title now and the capsule would sit over it; returning to the
Browse menu restores it.

## Explore chips clear of the status bar (2026-11-14)

The per-source Explore page's filter chips sat only 8.dp below
the status bar and read as touching it, like the title capsule
did before its fix. The chips row's top padding is now 20.dp,
matching the capsule's clearance, so the chips sit clearly
below the status bar. The bottom padding stays 8.dp.

## Manga detail: backdrop fades into the page (2026-11-14)

The detail header's cover-color wash was a solid 0.25-alpha
rectangle, so where the header ended and the action row below
began there was a hard line. The wash now fades out over the
bottom 30% of the header (solid for the top 70%, then to
transparent), so the backdrop blends into the page background
instead of ending in a cut. The blurred cover image already had
a vertical gradient fading it into the background; only the wash
was solid.

## Capsule clear of the status bar; version 0.2.0 (2026-11-14)

The title capsule is one HomeScreen overlay, so this fixes every
page at once. Centered in the bare 64.dp band it sat only ~12.dp
below the status bar and read as touching it, especially on pages
whose own toolbar is gone (the user saw it on Extensions). The
capsule band now has 8.dp of extra top padding below the status
bar, so the capsule sits ~20.dp below it. `TopCapsuleClearance`
grew from 64.dp to 72.dp to match, and the Browse menu / More
top blank band went from 64.dp to 72.dp too, so their content
still clears the lowered capsule. Version bumped to v0.2.0
(versionCode 42).

## Top gap on Library, Updates and History (2026-11-14)

With the Mihon top-bar container gone at rest, the first row of
covers sat right at the top and the floating title capsule cut
into it. The three pages now have the same top gap as the other
pages (Browse menu / More) and the Explore page: a shared
`TopCapsuleClearance = 64.dp` of plain padding above the first
item, so the first row clears the capsule band. It is padding, not
a toolbar band, so when the list scrolls the covers move up
through it and stay visible instead of hiding behind a solid bar.
History's toolbar is now conditional too (only when searching),
like the Library and Updates ones, so all three pages match.

## Top bar container removed on Library, Updates and Explore (2026-11-14)

The title capsule stays (it is the Miuix addition, not the problem).
The thing cutting off the covers is the original Mihon top-bar
container — the toolbar band that sits above the content. It is
gone at rest on all three pages:

- Library: the toolbar only appears when searching or selecting,
  when its field / action mode is actually needed. The title lives
  in the floating capsule and search in the floating action capsule.
- Updates: the toolbar only appears in selection mode, when its
  action mode is needed.
- Explore: the per-source toolbar is gone (back goes through the
  system back gesture; the source title lives in the floating
  capsule). Only the Popular / Latest / Filter chips stay at the
  top.

## Dead code removed (2026-11-14)

Seven leftovers from the Miuix refactor, all verified
unreferenced before removal (delete 95b9e2fc, code 6f1d5e7b):

- `TabbedScreen` — the old tabbed Browse page, no callers
  since Browse became a first-level menu. Only the
  composable was removed; `TabContent` in the same file is
  still used by the Browse sections.
- `LibraryTabs.kt` — the old Library category tabs, removed
  remotely earlier; the local file was a leftover, now
  deleted locally too.
- `BrowseSourceToolbar.kt` — the per-source top bar;
  BrowseSourceScreen builds its own top bar inline now.
- `LibraryViewModel.State.showCategoryTabs` and its
  plumbing — the `DisplayPreferences` field, the combine
  source and the assignment; nothing read the state. The
  `categoryTabs` preference itself stays (it is live in the
  Library display settings).
- `UpdatesViewModel.lastUpdated` — the property, left over
  after its screen argument was removed.
  `LibraryPreferences.lastUpdatedTimestamp` stays: it is
  still written by the update worker and may be part of
  backup/restore.
- `LocalFloatingBottomBarTabScale` — never provided
  anywhere, always the default 1f; the declaration, the
  read and the scaleX/scaleY lines are gone.
  Behaviour-neutral.
- The commented-out `SDK_INT < P` check in
  `AppUpdateChecker`, with its comment.

## Cover cards: smaller radius, tonal step for depth (2026-11-14)

The compact-grid card (Library and Browse source grids)
and its cover went from 12.dp to 8.dp — the card clip, the
cover's top corners, and the cover-only full rounding, so
the two grid modes stay consistent. The comfortable-grid
cover keeps its own 12.dp default from `MangaCover.Book`,
and the card title and outline are unchanged.

For depth, the card background is now
`surfaceContainerHighest` instead of `surfaceContainer` —
one tonal step above the page, so the card reads as a
raised layer (lighter in dark mode; a light-grey tile in
light mode). A 1.dp top highlight was tried first
(7aa97f03) and replaced by this.

## Extensions: trust dialog unreachable under the floating bar (2026-11-14)

Tapping 信任 on an untrusted self-installed extension did
nothing. The trust chain itself is fine — the dialog, the
ViewModel, `TrustExtension` and the reload on the preference
change, and upstream #4071 only refactored where the store list
comes from without changing the trust key.

The first diagnosis (the floating bar covering the dialog's
buttons) was wrong: the Extensions page is pushed onto the main
navigator, so HomeScreen — and its bar — is not composed there
at all. The `LaunchedEffect(notLoadedState)` bar-hide it
produced (b086dd8d) parked on a rendezvous channel with no
collector and did nothing. The real cause is the one found on
the Explore and manga detail pages: the trust dialog is a Miuix
OverlayDialog and the page had no Miuix Scaffold ancestor, so
it never rendered. The fix (2026-11-15, 506dac8c) is the same
layout-neutral Miuix Scaffold wrapper over BrowseSectionScreen,
and the dead bar-hide is removed. See "Extensions: trust dialog
finally has a host" below.

## Explore: the page sits outside HomeScreen — no capsule, no bar (2026-11-14)

The Explore page (and the whole Browse section flow) is
pushed onto the main navigator, not a per-tab navigator —
tabs have no Navigator of their own, and MainActivity's
root Navigator shows only its top screen. So when the user
opens a source page, HomeScreen leaves the composition
entirely: its bottom bar, title capsule and action capsule
do not exist on that page at all. That is also why the
bottom bar is absent in the screenshot, and why reporting
capsule actions to HomeScreen (`setCapsuleActions`) could
never show a capsule here, no matter the scroll state —
the earlier scroll explanation and the collector reset
(4a283c80) were aimed at the wrong mechanism.

`BrowseSourceScreen` now owns its capsule locally: the
actions are computed in the composable body (search +
cover layout, empty while searching, exactly what it used
to report), with a local scroll-hide connection (120.dp,
same as HomeScreen's) on a Box wrapping the content and
the capsule rendered as an `AnimatedVisibility` sibling
over the list, using the same bar style preference and
`rememberLayerBackdrop` handle (the Box records the layer
when glass is enabled). The dead `HomeScreen` calls on
this page are gone (title capsule, bar hide, action
report). Build fix: the first push missed the
`tachiyomi.presentation.core.util.collectAsState` import —
`barStyle` is a Preference, so the runtime `collectAsState`
(Flow/StateFlow only) could not resolve it; the same gotcha
as LibraryTab/UpdatesTab.

The long-press remove fix (6b54b411) is ineffective on
this page for the same reason: there is no floating bar
here to hide, so the 移除 button was never covered by it.
Whether removing works now, and whether the remove dialog
even opens, is still open — it needs the user to test
after rebuilding.

## ChangeCategoryDialog: Miuix card list (2026-11-15)

The 设置分类 dialog (opened from the Library page's
selection mode, and from Browse/History/manga detail) had
two layout defects: its category rows were bare — no
vertical padding at all, so the checkbox rows touched each
other and the tap target was barely the checkbox height —
and its bottom button Row had no `fillMaxWidth`, so the
`Spacer(weight = 1f)` between 编辑 and 取消/确定 got no
space and all three buttons bunched up on the left.

Now the rows live in one Miuix `Card`, like the reader
settings groups: each row is 16.dp horizontal / 12.dp
vertical padding, the label takes the remaining width
(`weight(1f)`, 8.dp after the checkbox). The Card colour
is `surfaceContainerHighest`, not the default
`surfaceContainer` — the dialog background is
`surfaceContainer` in both themes (white in light,
#242424 in dark), so a default Card would vanish into
it; one shade up stays visible in both. The button Row
is `fillMaxWidth` with 12.dp top padding, so 编辑 sits
left and 取消/确定 sit right. The empty-state branch
(no categories) is unchanged; the create/rename/delete
dialogs in the same file already used proper button rows.

## Reader toolbars as floating pills (2026-11-15)

The top and bottom bars were full-width sharp bands floating
over the page — the only such bands left in an app that is
otherwise all floating capsules — so they read as leftover
Mihon chrome. Both are pills now, in the same language as
the title and action capsules: 24.dp corners, translucent
`surfaceContainer` unchanged, side margins 12.dp on phones
and 24.dp on tablets, 8.dp clear of the status bar at the
top and of the navigation bar at the bottom.

The top bar no longer uses the shared `AppBar` (its band,
with its own status-bar padding, cannot float). `ReaderTopBar`
builds the same row directly — 64.dp, back button, title
plus chapter subtitle, bookmark and overflow — reusing the
shared `AppBarTitle`, `UpIcon` and `AppBarActions` pieces,
so only the container changed; tapping the pill still opens
the manga page. The bottom pill is 48.dp tall (4.dp vertical
padding around the icons, like the floating capsule). The
chapter navigator pill's side padding went from 8.dp to
12.dp on phones so its edges line up with the bottom pill;
the vertical navigator keeps its place at the side. The
prev/next buttons also stopped forcing their disabled
container to the same colour as the enabled one, so a
missing chapter now reads as disabled. Viewer, gestures,
tap zones and fullscreen are untouched.

## Miuix migration: dialogs, first batch (2026-11-15)

The full inventory lives in `MIUIX-MIGRATION.md` (local, not pushed):
202 files still import Material3. Order is shared foundation first,
then dialogs, then primitives area by area, theme last; the blockers
left in Material3 are the tablet `NavigationSuiteScaffold`, the tracker
`DatePicker`, Monet dynamic colour and the wavy progress indicator.

First batch (efcd4e8a): three small `AlertDialog`s became
`OverlayDialog` + Miuix `TextButton`s, behaviour unchanged: `DeleteLibraryMangaDialog` (checkbox list kept — its
`LabeledCheckbox` already uses a Miuix `Checkbox`, only the label Text
is still M3), the inner `SetCoverDialog` in
`ReaderPageActionsDialog` (text-only, so the string became `summary`;
the outer AdaptiveSheet was already Miuix), and
`MigrationExitDialog`. Six `AlertDialog`s remain.

## Library selection bars as floating pills (2026-11-15)

Selecting manga used to call up two Mihon bars: the top selection
`AppBar` (full-width band, count + select-all/invert) and the bottom
`LibraryBottomActionMenu` (edge-attached `Surface`, long-press to
reveal labels). Both are floating pills now (b9649f49), like the
reader's bars:

- Top: `LibrarySelectionToolbar` builds its own 64.dp pill —
  12.dp side margins, 8.dp clear of the status bar, 24.dp corners,
  `surfaceContainer` at the reader's translucency (0.9 dark /
  0.95 light). Miuix `IconButton`s for cancel (Close) / select-all /
  invert, count in Miuix `Text` (`textStyles.title2`). The
  `statusBarsPadding` is kept: the old `AppBar` had it, so the pill
  sits where the band sat. Search toolbar untouched.
- Bottom: `LibraryBottomActionMenu` is a Miuix `Surface` pill
  (24.dp corners, `surfaceContainerHigh`, 12.dp margins, 8.dp bottom
  clearance, nav-bar inset outside the pill). The shared `Button`
  uses Miuix `Icon` + `Text` (`textStyles.footnote1`); its
  `combinedClickable` + Material `ripple` stay, as the chapter menu's
  comment already documents (Miuix `IconButton` has no long-click).
  The "more" overflow (migrate/delete) is an `OverlayDropdownPopup`
  now, same pattern as the capsule dropdowns. The download button's
  `DownloadDropdownMenu` stays (shared flyout, out of scope).
- The same pill treatment went to the chapter menu
  (`MangaBottomActionMenu`, same file, same `Button`): it is used on
  manga detail and Updates, and leaving it edge-attached would have
  looked inconsistent. Its callers pass a width fraction on tablets,
  which the outer `Box` keeps; the Library menu stays full-width.
- Updates followed (0b110595): `UpdatesAppBar` only ever renders in
  selection mode (the top bar is empty at rest), so its whole body
  became the same 64.dp pill, and its regular branch — tablet title
  plus filter/upcoming/refresh actions — was dead code and went away
  with the four parameters that fed it. Filter/upcoming/refresh
  already live in the floating action capsule. Its bottom menu was
  already the pill via `MangaBottomActionMenu`.
- 2026-11-15 (02fc61cd): the four selection pills went from 24.dp
  corners to 32.dp, matching the liquid nav bar (64.dp tall, 32.dp
  corners = fully rounded). Top pills in Library/Updates, both
  bottom menus. The reader's bars and the floating action capsule
  stay at 24.dp.
- 2026-11-15 (ff90a8b9): the Library bottom bar no longer expands
  from its own top edge — it slides down from the cover that was
  pressed. The three Library item composables report their cover's
  vertical centre in window coordinates (`reportCoverCenterY`, a
  no-op when no listener is attached, so Browse grids pay nothing);
  LibraryContent keeps the latest positions in a plain map and sets
  the anchor before toggling selection, so the anchor is known in
  the same recomposition that shows the bar; the bar's enter is
  `slideInVertically` from the cover plus a fade (350 ms), its exit
  slides back up to the cover (250 ms). The slide distance is the
  window height minus the bar height minus the anchor, so the bar's
  top edge starts exactly on the cover's centre. The chapter menu
  keeps the old bottom-expand (no anchor there).
- 2026-11-15 (d961910e): manga titles moved from below the cover
  onto a frosted glass strip over the cover's bottom edge
  (`CoverTitleStrip`, in both grid items). The strip is a
  translucent veil over the cover — surface at 0.45 alpha at the
  top, 0.8 at the bottom — with a 1.dp white rim light along its
  top edge, the title in bodyMedium Medium with a soft veil-coloured
  shadow. No real blur: the strip sits inside the page's recorded
  backdrop layer, where glass would recurse (the same reason the
  floating bar lives outside that layer); the look is the launcher
  icon's frosted disc in strip form. The compact card's surface is
  gone — the card is the cover itself, full 8.dp rounding — and the
  continue-reading button now sits on the strip instead of under
  it. The list mode keeps its row layout, and the cover-only mode
  shows no strip at all.
- 2026-11-15 (0d836ddf): a 2.dp shadow around the manga cards
  (`Modifier.shadow` on the card, before its clip, in both grid
  items) so the cover lifts off the page background. Kept at 2.dp
  on purpose: a bigger elevation would be clipped by the
  interaction layer's 4.dp padding, and the ask was a little
  shadow. The list rows are not cards and got none.
- 2026-11-15 (baaf1a24): on the per-source Explore page, the
  source name's top padding went from 40.dp to 75.dp, and the
  Popular/Latest/Filter chips became physical push buttons
  (`FilterButton`): a raised vertical gradient with a 1.dp white
  rim light and a 2.dp shadow when up, pressed into a flat darker
  face with a dark inset top edge and no shadow when down — the
  blue primary selected state is gone, the pressed-in look is the
  active state now. 12.dp corners, 18.dp icon, same labels.
- 2026-11-15 (9b69d71f): the comfortable grid was merged into the
  compact grid. The two options presented the same result: both
  grids use the same column count (the Library's column
  preference / the Explore page's `getColumnsPreference`, neither
  depends on the mode) and, since the frosted title strip, the
  same card. `ComfortableGrid` is gone from `LibraryDisplayMode`
  (the test now expects 3 modes); old saved "COMFORTABLE_GRID"
  values deserialize to CompactGrid. `LibraryComfortableGrid`,
  `BrowseSourceComfortableGrid` and `MangaComfortableGridItem`
  are deleted; the Library pager, the Explore content switch,
  the display-mode pickers (capsule dropdown + settings dialog)
  and the capsule display icons lost the comfortable entry, and
  the global search row now uses the compact item.
- 2026-11-15 (b1df9064): the Explore page's filter, capsule and
  chips. The page is pushed onto the main navigator, outside
  HomeScreen, so it has no Miuix Scaffold ancestor — and Miuix
  overlays (dialogs, dropdowns) only render inside one, which is
  why the remove dialog silently never opened there (the
  long-press remove issue) and the display dropdown never
  appeared either. The whole page is now wrapped in a Miuix
  Scaffold (zero insets, the page background, layout-neutral),
  which hosts the overlays — this also revives the remove dialog.
  The filter is no longer a bottom sheet: the Filter action moved
  from the chips row into the capsule, and its panel is an
  `OverlayListPopup` from the capsule (320.dp wide, at most 70%
  of the screen height, Reset + Filter header, the same
  `FilterItem` form in a scrolling list; `FilterItem` is now
  internal and shared with `SourceFilterDialog`, which the
  migration search screen still uses as a sheet). The capsule
  keeps only the search tools (Search + Filter, with the accent
  tint on Filter while a search listing is active) — the cover
  layout action and its dropdown are gone, so the Explore page's
  layout stays on the stored mode. The Popular/Latest chips
  dropped the physical-button gradient for the capsule's
  material: a flat surfaceContainer pill (solid when opaque,
  0.75 alpha otherwise — in glass mode too, because the chips
  sit on the header's opaque surface with no page content behind
  them to blur, so the glass alpha would make the pill vanish),
  the active chip marked with the accent tint like the nav bar's
  active tab. The capsule hides while the filter panel is open.

## Remaining dialogs migrated (2026-11-15)

The six `AlertDialog`s left after the first batch became
`OverlayDialog` + Miuix `TextButton`s (02fc61cd), behaviour
unchanged: the extension uninstall confirmation
(`ExtensionsTab`), the reader's loading-adjacent-chapter indicator
(`ReaderActivity`, now a Miuix `CircularProgressIndicator` in the
dialog; its `Text` and the M3 indicator were used nowhere else in
the file), the shader import error dialog (`WebGpuViewerSettings`;
message stays a monospace `MiuixText` at 12.sp inside the scrolling
`SelectionContainer`, the file's other M3 buttons stay), and the
migration confirm/progress dialogs (`MigrationMangaDialog`,
plural title + skip text as `summary`; `MigrationProgressDialog`,
now a Miuix `LinearProgressIndicator` fed the animated float
directly, still not dismissable by back press or outside tap).
One deliberately stays Material3: the extension preference edit
dialog (`SourcePreferencesScreen.EditTextPreferenceDialog`) — its
comment records why: it must be a platform `Dialog` window so
extensions can find `android.R.id.button1` in its view hierarchy
and so the soft keyboard can be forced visible through the dialog
window; Miuix's `OverlayDialog` is an overlay popup, not a platform
window, so it cannot host either.

## Filter button: neutral instead of primary blue (2026-11-15)

The filter confirmation button was still blue on both filter pages
(9926f72d): the Explore filter panel (`BrowseSourceScreen`, the
`OverlayListPopup` from the capsule) and the filter sheet
(`SourceFilterDialog`, used by the migration search screen) both
used `ButtonDefaults.buttonColorsPrimary()` for 筛选. Both now use
`buttonColors()` — the neutral `secondaryVariant` fill, in line with
the chips losing their blue primary state. The button stays a filled
button, so it still reads as the confirm action next to the Reset
`TextButton`.

## Manga detail: favorite toggle needs its remove dialog (2026-11-15)

Tapping 收藏 a second time on the manga detail page did nothing.
The tap itself is fine: `onAddToLibraryClicked` calls
`showRemoveFromLibraryDialog()` when the manga is already a
favorite, and the confirm dialog calls `toggleFavorite()`. The
dialog is the part that never appeared — `RemoveMangaDialog` is a
Miuix `OverlayDialog`, and in `ui/manga/MangaScreen.kt` the whole
dialog `when` block is rendered in `Content()` as a sibling of the
presentation `MangaScreen()` call, outside the Miuix `Scaffold`
that lives inside that call. Pushed screens have no other Miuix
Scaffold ancestor, so the overlay had no host and silently never
rendered. Fixed in 241d7be4 by wrapping the page body (after the
loading early return) in the same layout-neutral `MiuixScaffold`
as the Explore page — zero insets, page background — which hosts
the remove dialog and the other `OverlayDialog`s rendered in the
same block. Mechanics checked in the Miuix source first: a nested
Scaffold shares its parent's root dialog states
(`parentRootDialogStates ?: dialogStates`), and a dialog with
`renderInRootScaffold` registers into that root list, which the
outer wrapper's popup host renders. Untested — needs a rebuild
and a real unfavorite tap to confirm.

## Manga detail: start button as a nav-bar pill (2026-11-15)

The 开始/继续 button was still the blue Material3
`SmallExtendedFloatingActionButton` (c4b2156a). It is now a
`MangaStartButton` pill in exactly the floating capsule's
material, in both the phone and tablet layouts: surfaceContainer,
0.75 alpha when the bar style is translucent, and 0.4 alpha +
the same vibrancy + 4.dp blur + tilt highlight when it is liquid
glass. The bar style is read once in `MangaScreen` (with the
usual glass-unsupported fallback to translucent) and passed to
both implementations with one shared `LayerBackdrop`; the
`PullRefresh` content records the layer in glass mode, so the
pill blurs the chapter list behind it while the button itself
stays outside the recorded layer. Behaviour is unchanged: the
show/hide animation, the expand-to-icon-only collapse while
scrolling, the 56.dp height the list's bottom clearance is
measured from, and the onSurface play icon + label. Untested —
needs a rebuild and a visual check.

## Chapter list: breathing room (2026-11-15)

The chapter rows were tight: 12.dp of padding above and below
each row, 6.dp between the title and the date/scanlator line,
and the "X 话" header sat only 4.dp off the description card
above it (70db0274). Now the row pads 16.dp top and bottom, the
title-to-subtitle gap is 8.dp, and the header carries 12.dp
above it and 8.dp below — a clear section break after the
description card and air before the first chapter. The inset
divider stays where it was (8.dp each side), so it now sits
24.dp from the subtitle above and the title below instead of
20.dp, and the full gap between two chapters is 48.dp. The
missing-chapters gap row is untouched: it is a thin divider
with a label, not a content row. Untested — needs a rebuild
and a visual check.

## History cards use the Library card (2026-11-15)

The History waterfall cards were still the old form: a
surfaceContainer card, cover with top-only 12.dp rounding,
title + chapter/time below it, and the favourite/delete row at
the bottom (5d799307). They now use the Library's
`MangaCompactGridItem` directly, so the cover, the frosted
title strip, the 8.dp rounding and the 2.dp shadow are the same
component in both places — a change to the Library card lands
on History too. Tap the cover to open the manga; the strip's
play button resumes reading. What the Library card doesn't
have stays history-specific, in a slim row under the cover:
the chapter/time line (bodySmall, secondary) on the left, and
the favourite (only when not in the library) and delete
IconButtons on the right; tapping the row itself resumes
reading, like the old card's body click. Long-press does
nothing on history cards — there is no selection mode there.
Untested — needs a rebuild and a visual check.

## Floating UI: one motion language (2026-11-15)

Every floating element used its own timing: the title capsule and
the action capsules faded on AnimatedVisibility's defaults, the
bottom bar slid on a spring, the reader bars used their own
200/150 ms linear tweens, the Library bottom bar slid on 350/250
ms tweens and fell back to an expand with a 300 ms delay, and the
selection pills at the top popped in with no animation at all
(1d963c9b). A new shared `FloatMotion` object
(`presentation/miuix/FloatMotion.kt`) holds the one language:
enter 300 ms, exit 250 ms, FastOutSlowIn easing, with Fade,
Slide (IntOffset) and Size (IntSize) specs. Everything floating
now uses it: the title capsule, the bottom bar and the action
capsule on HomeScreen; the Explore page's local capsule; the
reader's top bar, bottom bar and chapter navigator (their
private readerBars specs are gone); the chapter menu and the
Library bottom bar (the cover slide is 300 ms in now, and the
no-anchor fallback expands immediately instead of waiting
300 ms); the long-press label reveal in the bottom menus; and
the Library/Updates selection pills, which now slide down from
the status bar and fade in when selection starts (their top
bar unmounts when selection ends, so only the enter plays).
Deliberately unchanged: the manga detail start button, whose
M3 animateFloatingActionButton keeps the layout measured while
hidden — the list's bottom clearance depends on that — and the
tab switch fade-through (200 ms), which is navigation, not
floating UI. Untested — needs a rebuild and a feel check.

## Extensions: trust dialog finally has a host (2026-11-15)

The Extensions page (inside BrowseSectionScreen, pushed onto the
main navigator) had no Miuix Scaffold ancestor, so its Miuix
OverlayDialogs — the trust dialog and the not-loaded dialog —
silently never rendered (506dac8c). The whole BrowseSectionScreen
content is now wrapped in the same layout-neutral Miuix Scaffold
as the Explore and manga detail pages (zero insets, page
background), which also hosts any Miuix overlay on the Sources
and Migration sections. The b086dd8d bar-hide is removed with it:
it was the wrong fix (no bar exists on a pushed page) and its
LaunchedEffect + HomeScreen import are gone. Untested — the user
must rebuild and try trusting a self-installed extension.

## Unfavorite directly, no confirm dialog (2026-11-15)

Tapping favourite a second time used to open the remove
confirmation dialog (14fac64a). Now the tap calls
`toggleFavorite()` straight away and the manga leaves the
library at once. The now-dead pieces went with it:
`showRemoveFromLibraryDialog()`, the `Dialog.RemoveFromLibrary`
entry and its branch in the dialog `when`, and the
`RemoveMangaDialog` import in ui MangaScreen. The
`RemoveMangaDialog` component itself stays — the Explore page
still uses it for long-press remove. One thing to know: if the
manga has downloaded chapters, removing it still shows the
existing snackbar asking whether to delete the downloads; that
is about the files, not a second confirmation of the removal.
Untested — needs a rebuild and a tap test.

## Explore long-press removes directly, no confirm dialog (2026-11-15)

Same change on the Explore page (e6b53ce5): long-pressing a
manga that is already in the library used to open the remove
confirmation dialog; now it calls `changeMangaFavorite()`
straight away. The now-dead pieces went with it: the
`Dialog.RemoveManga` entry, its branch in the dialog `when`,
the `RemoveMangaDialog` import — and `RemoveMangaDialog`
itself, deleted (local rm + remote delete 6d9015c4), since the
manga detail page had already stopped using it in 14fac64a and
nothing else called it. The duplicate-manga and
change-category dialogs on the same long-press path are
untouched. Untested — needs a rebuild and a long-press test.

## Updates cards unified with the Library card (2026-11-15)

The Updates page's waterfall card is now the Library's compact
grid card (78d88657), and the sweep found it was the last one —
Library, History, Explore and global search already use
`MangaCompactGridItem`, so every manga card in the app is the
same component now. The surface-card background is gone; the
title sits on the frosted strip over the cover's bottom edge.
The update-specific parts stayed: the unread dot / bookmark on
the cover's top start, the download indicator moved from the
cover's bottom end to its top end, and the chapter line (name +
read progress) in a slim row under the cover — tapping that row
does the same as tapping the card. Selection is now the
Library's frame + dimmed cover instead of the old 2.dp primary
border. Two behaviours kept by extending the shared card: a
separate cover tap (opens the manga page) via a new optional
`onClickCover` on `MangaCompactGridItem`, passed through to
`MangaCover.Book`'s `onClick`; and the strip's play button opens
the chapter. Small trade-offs to know: a read update's title no
longer dims on the strip (the chapter line below still dims and
the unread dot still marks unread items), and the title may now
run to two lines on the strip instead of one. Untested — needs
a rebuild and a look at the Updates page.

## Extension details: equal-size action buttons (2026-11-15)

On the extension info page (45fe3520) the 解除安装 and 程序信息
buttons are now the same filled Miuix Button, each `weight(1f)`
in a `fillMaxWidth` row. The mismatch: uninstall was a
`TextButton` (17.sp Miuix button text) while app info was a
`Button` with a plain M3 `Text` (16.sp / 24.dp line height), so
the two came out different heights despite the same 40.dp
minimum. Uninstall keeps the neutral `buttonColors()` grey it
already had; app info keeps its blue `buttonColorsPrimary()`.
The page's other parts (M3 app bar, settings widgets) are
untouched — they belong to the migration inventory. Untested —
needs a rebuild and a look at the extension info page.

## Glass-mode crash entering a source page (2026-11-15)

Tapping a source in liquid glass mode crashed the app on
entering its Explore page (42c18509). Cause: the floating action
capsule — which draws the recorded backdrop with blur — sat
INSIDE the `Box` that records that backdrop with
`layerBackdrop`, so it blurred itself: infinite recursion in
`RenderNode::prepareTreeImpl`. The capsule's own KDoc states
the rule; HomeScreen already follows it (bar and capsules are
siblings of the recorded layer). Fixed the same way here: the
capsule is now a sibling of the recorded content Box, in an
outer `Box`, with the scroll connection staying on the content
Box. The manga detail page was checked too and is fine — its
`layerBackdrop` sits on the PullRefresh content while the start
button lives in the Scaffold's floating-action-button slot,
outside the layer. Untested — needs a rebuild and entering a
source in glass mode.

## Top gradient blur fade (2026-11-15)

Scrolling content used to be cut off hard at the top edge; now
it dissolves (5411b7cb). New shared `TopFadeBlur`
(`presentation/miuix/TopFadeBlur.kt`): a strip at the top whose
veil is a vertical gradient from the page colour to transparent,
and in liquid glass mode the content behind it is also
progressively blurred through Miuix's `ProgressiveBlur.Top` —
full blur at the top edge, pixel-sharp at the bottom — via
`progressiveBlur` + `drawBackdrop`'s `progressiveGradient`.
The plain gradient background stays as the veil in every mode,
so the fade still works where the blur is off or unsupported.
Like the capsules, it consumes the shared backdrop, so it sits
OUTSIDE the recorded layer, under the floating elements: on the
tab pages it spans the capsule band (status bar +
TopCapsuleClearance, ending exactly where the list content
begins) and sits under the title capsule; on Explore it is a
48.dp strip under the top bar, its veil matching the top bar's
surface colour. Untested — needs a rebuild and a scroll on the
Library and Explore pages; the 8.dp full-strength blur radius
is a first guess, say if it should be softer or stronger.

## Selected grid card frosted in glass mode (2026-11-15)

In liquid glass mode a selected manga card read as almost
transparent — the cover dimmed to 0.76 over the selection tint
(1da8a172). Now, in glass mode only, the selected cover keeps
full alpha and gets a frosted veil instead: a milky white overlay
at 0.4 alpha over the cover, under the title strip and with the
badges drawing over it so they stay crisp. No real blur — the
card sits inside the page's recorded backdrop layer, where glass
would recurse (same reason as the title strip). The bar style is
read in `MangaCompactGridItem` itself (the same
`uiPreferences.barStyle` collect as the screens), so every grid
using the card — Library, Updates, History, Explore, search —
gets it without threading the setting through each screen. The
secondary frame and the non-glass look (dim + tint) are
unchanged. Untested — needs a rebuild and a long-press select
in glass mode; the 0.4 veil is a first guess, say if it should
be milkier or clearer.

## Side action capsules get the bar's lens refraction (2026-11-15)

The side capsules (Library/Updates/History tabs and Explore —
both are `MiuixFloatingCapsule`) already matched the title
capsule exactly (blur 4.dp + vibrancy + tilt highlight +
surfaceContainer 0.4), but the bottom bar has one more thing:
the `lens()` refraction that bends the content behind it at the
edges — the part that reads as "liquid" rather than plain
frosted blur (b0545123). The capsule now adds the same lens
(refraction 24.dp / 24.dp, as the bar), so all three floating
pieces share one material. The capsule's 24.dp rounded shape is
a CornerBasedShape, which the lens needs; the icons draw on top
of the refracted backdrop, so they stay undistorted. The bar's
drop shadow and the selected-tab pill's inner shadow were not
copied — those are shadows, not the glass itself, and the title
capsule has neither. Untested — needs a rebuild and a look at a
side capsule over covers in glass mode; on the narrow capsule
the 24.dp refraction may read stronger than on the wide bar,
say if it should be gentler.

## Liquid glass, stronger frost and one shared recipe (2026-11-16)

The manga start button's glass looked wrong: it was the only floating
glass piece without the bottom bar's lens refraction and drop shadow,
so it read as a flat translucent pill, and the glass itself was thin
— 4.dp blur and a 0.4 surface veil over the dense chapter list let
the text bleed through. The frost is now stronger everywhere, from
one shared recipe in `LiquidGlass.kt`: `MiuixGlassBlurRadius = 8.dp`
(the radius the top fade already uses at full strength) and
`MiuixGlassSurfaceAlpha = 0.55f`. The bottom bar, title capsule,
side capsule and start button all use them; the start button also
gains the bar's 24.dp / 24.dp lens and its 10.dp drop shadow
(0.2 dark / 0.1 light). Translucent stays 0.75 and opaque stays
solid; only the glass numbers moved.

The reader joins the same material (3010e50f): `ReaderActivity`
reads the Appearance bar style (same API-32 gate as the manga
page), records the viewer into a shared `LayerBackdrop`, and the
top bar, bottom bar and chapter navigator — all floating 24.dp
pills — draw the glass over it (blur + vibrancy + lens + tilt
highlight + 0.55 veil). The bars sit as siblings after the viewer
in the root Box, outside the recorded layer, so they never blur
themselves. In opaque/translucent modes the viewer is not recorded
and the pills keep the solid/0.75 surface. Viewer, gestures, tap
zones, fullscreen and the page indicator are untouched. Untested —
needs a rebuild: check the start button over the chapter list, the
reader bars over a page, and whether 8.dp blur costs anything
noticeable while scrolling.

## Lens depth, chromatic aberration, and a visible selected pill (2026-11-16)

The user asked to reference HyperLight's liquid glass. HyperLight is
an LSPosed module for HyperOS, not a library — its glass engine is
Haze's haze-glass — so what transfers is its parameter set, and our
lens already had the knobs: `depthEffect` and `chromaticAberration`
were only ever turned on for the selected-tab pill. They are now on
everywhere the lens is used, from one shared constant in
`LiquidGlass.kt`: `MiuixGlassChromaticAberration = 0.5f` (the pill's
old value). Bottom bar, side capsule, start button, reader bars and
chapter navigator all refract with depth + a prism fringe at the rim;
the title capsule deliberately keeps no lens, so its text is never
distorted at the edges. An engine swap to Haze Glass was considered
and interrupted before any code was written — not done.

The selected-tab pill also had a second problem: in glass mode its
background was effectively transparent. It refracts the page directly
and its surface only drew a 0.1f dark/light veil, so against the
bar's 0.55 veil it read as a clear hole and the selection was
invisible. It now draws a translucent accent tint
(`accentColor.copy(alpha = 0.2f)`) in `onDrawSurface` — one drawRect,
not a second glass pass (the earlier second-pass attempt cost too
much GPU time), in the same alpha family as the non-glass pill
(accent 0.15f). Untested — needs a rebuild: check the glass over
busy covers (depth + dispersion cost), and whether the pill reads as
the selected tab in both themes.

## Top fade blur 12.dp (2026-11-16)

The top fade's progressive blur was 8.dp — a first guess from when
the fade was added. The user asked for the top-of-page shadow to be
12, so the fade's blur radius is now 12.dp (TopFadeBlur.kt only; the
shared glass recipe stays at 8.dp for the floating pieces). The
fade's veil gradient and its height (72.dp on tab pages, 48.dp on
Explore) are unchanged. Untested — needs a rebuild: check the fade
over busy covers and whether 12.dp costs anything noticeable while
scrolling.

## Top fade blur back to 8.dp (2026-11-16)

12.dp was tried and reverted the same day at the user's request —
the fade is back at 8.dp, and the shared recipe comment in
`LiquidGlass.kt` is restored to match. No other change.

## User-tuned liquid glass recipe (2026-11-16)

The user edited the six glass files by hand (sent back as
LiquidGlass_OS_Complete_v2_0_udda.zip) and asked for them to be
applied globally — applied as-is, plus two comment-only fixes (the
shared-recipe KDoc and the title capsule's "no lens" note, both stale
against the new values). The recipe is now lighter: blur 6.dp,
surface veil 0.26, chromatic aberration 0.14, vibrancy saturation
1.18; the specular highlight is wider and softer (stroke 1.15.dp at
0.92 alpha, inner white 0.17, inner blur 2.8.dp, light intensities
0.82 / 0.26). The dispersion shader is rewritten — 3 RGB samples
instead of 7 spectral ones, with a Fresnel-like edge factor, so the
prism fringe grows towards the rim; circleMap now clamps its input
and guards the sqrt, so no NaN at the edges. Per-piece lens values
moved too: bottom bar 18.dp / 12.dp, side capsule 16.dp / 10.dp
with a press-scale spring on its buttons, title capsule gains a very
shallow lens (8.dp / 4.dp, chroma at a quarter of the shared value),
selected pill lens now (8+4*progress, 7+5*progress) with a lighter
accent tint (0.11 + 0.035*progress) plus a faint white lift, inner
shadow 18.dp / 0.12 with a 0.65 vertical offset, top fade blur
10.dp. The start button and reader bars/navigator were not in the
zip — they follow the shared constants, so they pick up the lighter
recipe while keeping their 24.dp lens. Untested — needs a rebuild:
check the lighter veil over busy covers (text legibility), the
capsule's shallow lens on the title, and the capsule press spring.

## Chapter download button removed; start button always shows (2026-11-16)

Two manga-detail fixes in one push. First, the per-chapter download
button is gone: the ChapterDownloadIndicator is removed from
MangaChapterListItem, along with its three parameters and the
downloadProgressProvider threading through the presentation screen
(four parameter declarations, four pass-throughs, the item call-site
arguments) and the ui call-site argument; the ViewModel's progress
plumbing went with it (the downloadProgress map, the progressFlow
collect, the progress lines in updateDownloadState, getDownloadProgress
and the mutableStateMapOf import). The download state itself stays:
the swipe-to-download gesture still works (it reads downloadState,
not progress), batch download from the selection menu is untouched,
and the Updates page still shows its own download indicators
(ChapterDownloadIndicator.kt stays for that page). Second, the start
button: it used to hide whenever a manga had no unread chapters —
original Mihon behaviour (`isFABVisible = hasUnreadChapters`), which
is why opening a finished manga from the Library showed no button at
all. It now shows whenever the manga has chapters
(`state.chapters.isNotEmpty()`), in both the small and large layouts;
the label is "继续" only when some chapters are read and some are
not, otherwise "开始". Tapping it opens the next unread chapter as
before, and when everything is read it falls back to the first
chapter in reading order — new getFirstChapter() on the ViewModel
and on List<ChapterList.Item> (same filters and sort as
getNextUnread), wired into onContinueReading. Untested — needs a
rebuild: open a fully-read manga from the Library and check the
button is there and starts from chapter 1; check a chapter row has
no download button but still swipes to download.

## Start button gets the FAB's primaryContainer veil (2026-11-16)

The user said the start button was very dark and looked odd. Root
cause, verified against the theme: the button's veil was
surfaceContainer at the shared alpha, but in this theme
surfaceContainer is the same color as the page background (Miuix
stock scheme: dark #242424 / #242424, light white / white), so no
veil alpha can ever lift it off the page — it was a dark smudge that
only got worse when the shared veil dropped to 0.26. Fix: the button
now uses the original FAB's primaryContainer instead of the shared
veil — solid in opaque mode, 0.75 alpha in translucent and glass
modes, so a hint of the blurred page still shows through — and the
icon and label use onPrimaryContainer (M3 FAB convention). Blur,
lens, tilt highlight and drop shadow are unchanged; the button is the
only glass piece that does not follow the shared veil, and a comment
in the code records why. The now-unused MiuixGlassSurfaceAlpha and
MiuixTheme imports are removed from MangaScreen.kt. Untested — needs
a rebuild: the button should read as a pale pink (light) / olive
(dark) pill with dark-red / pale-yellow text.

## Chapter list in a Miuix card (2026-11-16)

The user picked the grouped-card direction for the chapter list
(the other options offered were a flat polish pass and grouping by
scanlator). The chapter section is now one Miuix card: each row
paints the card surface (Miuix surfaceContainer, the same color the
settings and detail-page cards use), 16.dp outer corners via
CardDefaults.CornerRadius, 12.dp side margins matching the detail
page's other cards, and the existing inset dividers between rows.
The list stays lazy — there is no single Card wrapping the rows
(that would compose every chapter at once); instead the list computes
one corner shape per row (rounded on the first/last row's outer
corners only) and each row draws the card surface clipped to it, so
the section reads as a single card. The swipe box's background is now
the card surface too, so a swiped row still reveals the card, and
the missing-chapters gap row gets the same dressing (its dimming
alpha applies to the inner content only, not the card surface). The
row's end padding is 16.dp now, symmetric (the 8.dp was for the
removed download button). The ChapterHeader's side padding moved from
16.dp to 28.dp — SmallTitle's horizontal margin — so the title lines
up with the chapter titles (12.dp card inset + 16.dp row padding).
Untested — needs a rebuild: check the card reads as one piece over a
long list (first/last corners, dividers, selection tint, swipe
actions, and the missing-chapters row all on the card surface).

## Liquid glass: WeKite recipe and a draggable pill that refracts the active tab (2026-11-16)

The user asked whether the liquid glass could match
SymonChu/WeKite's. Research first: WeKite's glass is the same engine
family as Tychii's (both adapted from Kyant0/AndroidLiquidGlass via
InstallerX-Revived), so there is no new engine to swap in — what
differs is the recipe and the bottom bar's selected pill. WeKite's
lens shader is the older 7-sample dispersion version; Tychii keeps
the user's own 3-sample Fresnel tune. WeKite's ViewBackdrop (sampling
a native View, for glass floating over WeChat's own views) does not
apply here — Tychii's content is all Compose. The user picked the
middle of three options offered: recipe + draggable pill (not just
parameters, not the full port with a slimmer bar and a blur-radius
setting).

Recipe (all in `LiquidGlass.kt`, shared by every glass piece): blur
6.dp → 8.dp, veil 0.26 → 0.4, vibrancy saturation 1.18 → 1.5.
The bar's lens is now 24/24 with no depth effect and no chromatic
aberration (was 18/12 + depth + 0.14). The shared chromatic
aberration stays 0.14 for the title capsule, side capsule, start
button and reader bars; the pill gets its own constant,
`MiuixGlassPillChromaticAberration = 0.5f`. The tilt-following
specular highlight is unchanged (WeKite uses Miuix's built-in
Highlight; ours is the more involved one).

Pill (in `MiuixLiquidBottomBar.kt`, new `liquid/CombinedBackdrop.kt`
ported from WeKite with its attribution header): the pill now samples
a combined backdrop — the page plus a second layer that records the
tab strip a second time with every tab drawn in the accent colour.
That layer is an invisible Row (`alpha(0f)`, semantics cleared, no
click handling) the same size and position as the pill (56.dp tall,
4.dp side padding), rendered with all tabs forced active via a new
`LocalBottomBarForceActiveTab` CompositionLocal that
`TychiiLiquidBar.kt` reads for the tab tint. The pill's lens is now
WeKite's: (10*pressProgress, 14*pressProgress, depth, chroma 0.5) —
zero at rest, opening only while pressing or dragging. Its surface
veil is WeKite's too (black/white 0.1 fading out with press, plus
black 0.03*press); the flat accent tint is gone, because the
selection colour now comes from the refracted active icon itself.
Inner shadow is 8.dp*press, black 0.15 (was 6.dp, 0.12).
Deliberately NOT ported from WeKite: the active layer records only
the icons, with no second blur/lens/veil pass — an earlier Tychii
version did the second glass pass and it cost too much GPU time;
the pill refracts the page directly, as before.

The drag itself is unchanged: the pill was already draggable
(DampedDragAnimation with the rubber-band panel offset, press bulge
and velocity squash came with the SukiSU port). What is new is what
the pill shows while dragging: the active-coloured icon refracting
inside it, instead of a flat tint.

Untested — needs a rebuild: check the frostier veil and stronger
saturation over busy covers, the pill at rest (clear lens, accent
icon refracted inside), the pill while pressing/dragging (lens opens,
chroma at the rim), and that a tap on an already-selected tab still
reaches the tab (the invisible active layer must not eat taps; the
visible content Row stays on top).

## Selected pill could not be dragged (2026-11-16)

The user reported the pill still could not be dragged, after the
WeKite change above. Root cause, verified in code: the tab content
Row was the last child of the bar's Box, so it covered the pill
entirely — touches over the pill hit the content Row first, and the
pill's `pointerInput` (the drag gesture) never received them. The
drag machinery itself was fine; it was unreachable. (The content Row
had been put on top deliberately, when the pill's glass used to paint
the blurred page over the selected icon and make it vanish. That
reason is gone now that the pill refracts the active icon through the
combined backdrop.)

Fix, code b6aa31e5: the pill Box is now the last child of the Box
(on top, WeKite's layering), so its drag gestures receive touches.
Because the pill now covers the selected tab, taps and long-presses
on it need forwarding: `DampedDragAnimation` gained `onTap` /
`onLongPress` callbacks — the tap/long-press detection is written
fresh here (accumulated drag vs touchSlop for a tap, a delay timer
for a long press), not copied from WeKite's GPL-3.0 file, since
Tychii is Apache-2.0. The bar gained `onTabReselected` /
`onTabReselectedLongPress` params (defaulting to `onSelected` / no-op,
read via `rememberUpdatedState` since the animation is remembered
once), and `TychiiLiquidBar` wires `onTabReselected` to `onTabClick`,
so tapping the selected tab still means reselect (e.g. re-tapping
Library opens its settings dialog), exactly as if the tab itself had
been tapped. The selected icon still shows: it is refracted inside
the pill through the combined backdrop, as in the previous change.

Untested — needs a rebuild: drag the pill between tabs (it should
follow the finger, bulge while pressed, and snap to the nearest tab
on release), tap the selected tab (reselect behaviour, e.g. the
Library settings dialog), and check the selected icon stays visible
inside the pill while it sits on top.

## Selected tab is frosted glass, not clear (2026-11-16)

The user asked for the selected tab to read as frosted glass rather
than transparent liquid glass. The pill used to be clear: it sampled
the page sharp, with only a 0.1 veil, so the covers showed through
almost untouched.

Fix, code d0422650: the active layer (the invisible Row recorded into
tabsBackdrop) now draws the full glass pass while recording — blur
8.dp, vibrancy 1.5, lens 24/24, surfaceContainer veil 0.4, the same
recipe as the bar itself — and only then the accent-coloured tab
content on top. The pill samples that layer, so it shows a frosted
page behind a crisp icon. The icon stays sharp because it is the
layer's content, not its backdrop: blurring the pill itself would
have blurred the icon too, so the blur belongs in the recording, not
in the pill. This is exactly WeKite's stack (its overlay does the
same full pass); the earlier Tychii port had deliberately dropped it
to save a second blur pass, and this change takes that cost back —
one extra blur over a 56.dp-tall strip, the pill's own area. The
pill's own effects are unchanged (press lens + 0.1 veil on top), so
pressing/dragging still opens the lens over the frost.

Untested — needs a rebuild: check the selected tab reads as frosted
(page blurred and milked behind it, icon crisp and accent-coloured),
the unselected tabs still show the bar's clearer glass, and scrolling
stays smooth with the extra blur pass.

## Start button is frosted glass too (2026-11-16)

The user asked for the manga detail start button to be frosted glass
as well. It already blurred the page behind it, but its veil was
primaryContainer at 0.75 alpha — mostly a solid pastel pill, with
only a hint of the blurred page showing through.

Fix, code cbc2e00a: in liquid-glass mode the veil now uses the shared
alpha (0.4), so the blurred page shows through like frosted glass.
The veil colour stays primaryContainer, not the shared
surfaceContainer: surfaceContainer is the same colour as the page
background in this theme, so it could never lift the button off the
page (that is what made it read as a dark smudge before, fixed by
a6bc6a6c). Translucent mode keeps 0.75 — there is no blur there, and
the label needs the opacity — and opaque mode stays solid. Blur,
lens, tilt highlight and drop shadow are unchanged, and the icon and
label (onPrimaryContainer) stay crisp on top.

Untested — needs a rebuild: check the button reads as a frosted pill
(blurred page visible through a pale pink / olive veil, label crisp)
in both themes, and that it still stands out from the page behind it.

## Pause glass backdrop recording while scrolling (2026-11-16)

The user asked to optimise smoothness and reduce power, and picked
the recommended way: degrade the glass while scrolling, restore it at
rest (the other options were to lower the blur radius / drop a blur
pass, or a checklist first).

What the audit found: Miuix's blur already does the standard
optimisations (adaptive downscale, cached RenderEffect and colour
controls), the app's lens() already early-outs at zero refraction and
for interior pixels, and the tilt highlight is already snapped to a
0.05 grid. The remaining cost is inherent to live blur: while a page
scrolls, its content changes every frame, so the backdrop layer is
re-recorded every frame — the whole page is drawn twice per frame
(once to the screen, once into the layer), then blurred. Lowering the
blur radius would not stop that re-recording, which is the dominant
cost, so the lever is to pause the recording while the page moves.

How it works, code a640952a:
- HomeScreen and the Explore page: the existing nested-scroll
  connection (which already observes scrolling for the capsule) bumps
  a scroll tick on every scroll event; a LaunchedEffect sets
  glassPaused while events keep coming and clears it 150ms after the
  last one (MiuixGlassScrollIdleDelayMs in LiquidGlass.kt). While
  paused the content Box drops its layerBackdrop modifier, so nothing
  re-records; the bar, capsules and top fade keep sampling the backdrop
  and show its last frame — blurred, so the frozen frame is hard to
  tell from live while the list moves. Switching tabs clears
  glassPaused, so a new tab never starts on a stale frame.
- Manga detail page (both layouts): the chapter list owns its own
  LazyListState, so the PullRefresh layerBackdrop is gated directly on
  !chapterListState.isScrollInProgress — true during a drag and during
  a fling, false once settled. No debounce needed there.
- The reader is untouched: page turns are discrete, not continuous
  scrolling, so its backdrop re-records once per turn.

Not done: lowering the blur radius or dropping a blur pass (the user
first tapped that option, then chose this one instead). The glass look
at rest is unchanged — same blur 8.dp, veil 0.4, lens.

Untested — needs a rebuild: scroll a long Library list and a long
chapter list and check scrolling feels lighter, the bar and capsules
still read as frosted while scrolling (frozen frame, not black or
transparent), and that the glass goes live again within a beat of
stopping.

## Scroll-pause reverted; other optimisations audited (2026-11-16)

The user reported the scroll-pause optimisation caused rendering
problems, and asked to revert to the previous version and optimise
elsewhere instead. Reverted in ae7f51b8: the four files are back to
their pre-a640952a state (no glassPaused / scrollTick /
isScrollInProgress gating anywhere — verified by grep — and the
MiuixGlassScrollIdleDelayMs constant is gone too). The
frozen-frame-while-scrolling approach is dropped; do not bring it back
without knowing what it broke on the device.

Audit of the other places, done read-only before picking a next step:
- Library grids are missing item keys: LibraryCompactGrid and
  LibraryList both call items() with a contentType but no key, so items
  are matched by index — when the list changes (sort, filter, category,
  new updates) every visible item is rematched and recomposed instead of
  keeping its own composition. Updates and History already have keys.
  Fix would be key = { it.libraryManga.manga.id } in both files.
- Coil has no clear win: the ImageLoader is upstream defaults (memory
  cache 25%, fetcher 8 / decoder 3 threads), and covers are already
  downsampled to the cell size by AsyncImage. Forcing RGB_565 would
  halve cover memory but risks banding on gradients — not taken.
- Reader preloading is already tuned (pager offscreenPageLimit 1,
  continuous viewer preloads 3 pages ahead / 2 behind). No change.

Untested — the revert itself needs a rebuild to confirm the rendering
problem is gone.

## Library lists get stable item keys (2026-11-16)

After the scroll-pause revert the user picked the Library-key fix
from the audit. Both Library lists called items() with a contentType
but no key, so items were matched by index: a sort, filter, category
change or new update rematched and recomposed every visible card or
row instead of keeping each manga's own composition (and its loaded
cover).

Fix, code ae7b41d4: key = { it.libraryManga.manga.id } in
LibraryCompactGrid and LibraryList (the manga database id, stable
across list changes). Updates and History already had keys, so nothing
changes there. No visual change — the lists look the same; they just
stop redoing work when the list changes.

Untested — needs a rebuild: switch sort order or category and check
the list updates without the visible cards flickering or reloading
their covers.

## Brushed metal: a fourth bar style (2026-11-16)

The user asked for a skeuomorphic material as Tychii's signature.
After two rounds of discussion (first three theme directions, then a
correction: the material itself must be skeuomorphic, not the
decoration; then three materials — paper / leather / metal) the user
picked brushed metal, cold grey.

The material, code 7b68b6b3:
- New `presentation/miuix/MetalMaterial.kt`: a vertical micro-gradient
  base (light #ECEDEF→#D2D4D8, dark graphite #2C2E31→#1D1F21), fine
  horizontal brush lines (1px, black/white alternating, alpha 0.02–0.07
  from a per-line hash so the texture is stable across frames), a 1px
  chamfer (bright top edge, dark bottom edge, following the rounded
  outline), a static diagonal sheen (white 0.06, top-left), and engraved
  text/icons (dark cut + a 1px light burr below). All drawn in Compose
  with gradients and lines — no bitmap textures.
- `BarStyle.METAL` joins OPAQUE / TRANSLUCENT / LIQUID_GLASS; the
  Appearance setting lists it automatically (it iterates
  BarStyle.entries). Strings: base "Metal", zh-rCN "金属". Metal needs
  no API 32 gate, so it runs on any API level; the glass note still
  shows only for liquid glass.
- The five glass pieces render metal: bottom bar (metal surface, and
  the selected tab is a stamped recess — darker + inner shadow —
  instead of the glass pill; drag still works), title capsule (metal
  surface, engraved title), action capsule (metal surface, engraved
  icons), start button (metal surface + the bar's 10.dp drop shadow,
  engraved play icon and label), reader bars and chapter navigator
  (metal surface; the navigator's prev/next buttons take the darker
  recess tone). Shapes, layout and corner radii are unchanged.
- Metal is opaque, so nothing samples the backdrop in this mode: the
  layerBackdrop gates (HomeScreen, Explore, detail page, reader)
  already only record for LIQUID_GLASS, so recording is simply off —
  the per-frame re-record cost disappears. TopFadeBlur falls back to
  its plain gradient veil (its non-glass branch). The Explore filter
  chips stay flat (0.75 surfaceContainer) — they sit on the top bar,
  not floating.
- Deliberately not done: engraving the reader bars' icons and title —
  they come from the shared AppBar components (UpIcon, AppBarTitle,
  AppBarActions) used on other pages too, so engraving them would leak
  the metal treatment onto non-metal pages. The reader bars get the
  metal surface only.

Untested — needs a rebuild: switch the bar style to Metal in
Settings → Appearance and check the bar, capsules, start button and
reader bars read as brushed metal in both themes, the selected tab
reads as a recess with a crisp icon, the engraved text is legible,
and the glass options still work when switched back.

## Metal mode: independent switch buttons instead of the capsule (2026-11-16)

The user asked for the metal navigation bar to change from a capsule
to independent floating buttons, each popping out when pressed like a
switch. One correction first: there are 5 tabs, not 4 (Library,
Updates, History, Browse, More), so there are 5 buttons — 4 when the
Updates tab is hidden.

Done in code d2c0df39: new `presentation/miuix/MetalTabBar.kt`
(`MetalTabButtons` + `MetalTabButton`). TychiiLiquidBar renders the
buttons in metal mode and the capsule in every other mode. Each button
is 56.dp tall, 28.dp corners, weight(1f) with 8.dp gaps, in the same
12.dp/8.dp outer padding as the capsule. The selected button is raised
(4.dp up, shadow 10.dp→14.dp and deeper, 4% larger) on a spring
(dampingRatio 0.6, stiffness 500 — a little overshoot, like a switch
snapping into place); tapping sinks it slightly first (the capsule's
press spring, 0.975). Labels follow the show-labels setting, badges
stay on the buttons, icons and labels are engraved. No pill, so no
drag-to-switch — tap only; re-tapping a tab still means reselect
(e.g. Library opens its settings dialog).

The shadow sits outside the motion (like the capsule): the button
rises while its shadow stays on the page and grows. The metal capsule
treatment from 7b68b6b3 is gone — MiuixLiquidBottomBar lost its isMetal
parameter and its metal base/pill branches, and the metalRecess
modifier is deleted (the recess colour values stay, used by the
chapter navigator's buttons). The glass capsule is untouched.

Untested — needs a rebuild: switch to Metal and check the five buttons
read as separate metal switches in both themes, the selected one pops
out with a spring when tapped, the old one settles back, labels and
badges are right, and the glass bar still works when switched back.

## Explore listing chips follow the bar material (2026-11-16)

The user asked for the Popular / Latest chips on the per-source
Explore page to use the same material as the global bar style — in
metal mode they were still flat grey pills while everything else was
brushed metal.

Done in code e0de3d97: FilterButton (BrowseSourceScreen.kt) now
branches on the bar style like the floating pieces. Metal: the
brushed-metal surface with the icon and label engraved. Liquid glass:
the side capsule's blur, lens and tilt highlight, sampling the shared
backdrop — but the veil stays at the translucent alpha (0.75), not the
glass one (0.4): the chips sit on the header's opaque surface, so
there is no page content behind them to blur, and the glass alpha
would make the pill vanish (the earlier code said the same thing in
its comment; the glass edge now comes from the highlight and lens
instead). Opaque and translucent are unchanged. The backdrop is passed
down from the screen (it was already in scope at the call sites).

Untested — needs a rebuild: open a source's Explore page and check the
Popular / Latest chips read as metal in metal mode (engraved, legible)
and as frosted glass in glass mode, in both themes.

## Manga card title strip follows the bar material (2026-11-16)

The user asked for the manga cards to use the same material as the
global bar style. The card itself is the cover image, so the material
surface on it is the title strip over the cover's bottom edge.

Done in code 9a1c9fd0, in CommonMangaItem.kt (the shared card, so
Library, Updates, History, Explore and search all get it — the card
already read the bar style itself for the glass selection veil, and
now also takes isMetal from the same read). In metal mode the strip
is a brushed-metal surface (square corners — the card's clip rounds
the strip's bottom edge to the card's 8.dp), the rim light is skipped
(the metal chamfer is the top edge), the title is engraved, and the
continue-reading button on the strip is a metal disc with an engraved
play icon, keeping the resume colour like the detail page's start
button. In liquid glass mode the strip is unchanged — it is already
the frosted look, and a real blur is impossible there (the strip sits
inside the page's recorded backdrop layer, where glass would recurse).
Opaque and translucent are unchanged. The list-mode rows are not
cards — they stay a plain row on the page background, so they are
untouched (their continue button keeps the primaryContainer pill).

Untested — needs a rebuild: check the strip reads as metal in metal
mode (engraved title legible, the continue button a metal disc) and
is unchanged in glass mode, in both themes.

## Metal retuned to bright chrome (2026-11-16)

The user asked to optimise the metal material into a bright,
chrome-like one. The brushed version was a flat two-stop gradient
with fine horizontal lines; chrome is a polished mirror, so the
recipe changed in MetalMaterial.kt only (code f220047c):

- Base: the two-stop gradient is now a five-stop banded one —
  bright, light silver, a darker reflection band in the middle (the
  reflected horizon), a bright band, a darker bottom. Light:
  #FBFCFD / #E9EDF1 / #C4CBD3 / #F4F7F9 / #C7CED5. Dark (black
  chrome): #4A4E54 / #33363B / #1E2023 / #3D4046 / #26282C — also
  brighter than the old graphite, with the same band structure.
- The brush lines are gone entirely — chrome is a polished face,
  and the lines were the brushed look. (The loop and its hash are
  deleted, not just hidden.)
- Chamfer: top edge white 0.5 → 0.7, bottom edge black 0.35 → 0.45 —
  chrome edges are brighter and harder.
- Sheen: white 0.06 → 0.1, same top-left diagonal.
- Unchanged: the engraving (dark cut + white burr), the recess
  colours, the drop shadows, and every caller — the bar, capsules,
  start button, reader bars, chips and card strip all pick the new
  look up through the shared metalSurface. The header comment also
  drops a stale line (the selected tab is a raised switch now, not
  a stamped recess; the recess survives for the chapter navigator's
  buttons).

Untested — needs a rebuild: check the metal reads as bright chrome
in both themes (bright bands, no visible brushing, engraved text
still legible on the brighter base) and that the glass modes are
untouched.

## Metal retuned to satin aluminum alloy (2026-11-16)

The user asked to try aluminum alloy instead of chrome. Chrome is a
polished mirror (banded reflections, no grain); aluminum is a satin
face — even colour, fine grain, soft edges. Same file, same callers
(code 027ab59d):

- Base: the five-stop chrome bands are gone, back to a two-stop soft
  gradient — but brighter and flatter than the original brushed
  steel. Light: #F3F4F6 → #DCDEE1. Dark (anodised): #34373B →
  #222427.
- Grain: the fine lines are back, but subtler than the brushed
  steel's — alpha 0.01–0.03 (was 0.02–0.07), same 2.dp spacing and
  deterministic per-line hash, so the satin stays stable across
  frames.
- Chamfer: softer than chrome — top edge white 0.7 → 0.4, bottom
  edge black 0.45 → 0.3.
- Sheen: white 0.1 → 0.06, same top-left diagonal.
- Unchanged: the engraving, the recess colours, the shadows, and
  every caller. The chrome band values are recorded here only; the
  code keeps one metal recipe at a time.

Untested — needs a rebuild: check the metal reads as satin aluminum
in both themes (even, bright, fine grain, no mirror bands) and that
the glass modes are untouched.

## Card titles engraved in metal mode (2026-11-16)

The user asked whether the manga card's title could be a real
engraved effect in metal mode. The strip already used the shared
engraved shadow — but that is only a light burr below the glyph, and
on the bright aluminum base white-on-near-white is nearly invisible,
so the title read as plain dark text.

Done in code db27f00c: a new shared helper, MetalEngravedText
(MetalMaterial.kt), draws the title twice — a dark copy offset 1px
up (the shadowed top edge of the cut) and the main glyph with the
light burr below (the lit bottom edge). Light from above, dark on
top, light at the bottom: the cross-section of a carved letter.
CoverTitleStrip uses it in metal mode (same two-line wrap and
ellipsis); the non-metal title keeps its soft halo shadow. The bar
and capsule titles keep the single-shadow engraving for now — only
the card title was asked for.

Untested — needs a rebuild: check the card titles read as carved
into the metal in both themes (dark rim on top, light rim below the
strokes), and that the two-layer text still wraps and ellipsises
like the single one.

## Metal as Smartisan-style hardware (2026-11-16)

The user asked whether the metal could become Smartisan OS style,
and picked the skeuomorphic-hardware direction (the other two
offered were the clean system UI and metal with a Smartisan-red
accent). The idea: the bar stops being "a metal material" and
becomes a realistic hardware object, like the metal parts in
Smartisan's icons — a polished aluminum face, light on top, a
bevelled edge, a shadow below.

Done in code 74e82d96, in MetalMaterial.kt only (all metal pieces
pick it up through the shared metalSurface):

- A top highlight band: a soft white light fading from the top edge
  to transparent at 30% down (white 0.18) — light glinting off the
  top of a real metal bar. The flat aluminum face was missing
  exactly this.
- A crisper bevel: top edge white 0.4 → 0.6, bottom edge black
  0.3 → 0.4 — a real bevel is bright on top and shadowed below,
  like a cut edge.
- Unchanged: the aluminum base gradient, the fine satin grain, the
  diagonal sheen, the engraving, the recess colours and the drop
  shadows.

Untested — needs a rebuild: check the metal reads as a physical
object in both themes (bright top face, hard edge, soft shadow) and
that the glass modes are untouched.

## Build fixes

- 2026-11-16: the metal material used the wrong `Shadow`: there are
  two classes with that name — `androidx.compose.ui.graphics.Shadow`
  (the text shadow `TextStyle` takes) and
  `androidx.compose.ui.graphics.shadow.Shadow` (the drop shadow the
  `dropShadow` modifier takes). `metalEngravedShadow()` returned the
  drop-shadow one, so `TextStyle.copy(shadow = ...)` rejected it at
  MangaScreen:383, MetalTabBar:153 and MiuixCapsuleTitle:99 ("Argument
  type mismatch ... `Shadow?` was expected"). Fixed by importing the
  text `Shadow` in MetalMaterial.kt. The same file also missed the
  `clipPath` import (`androidx.compose.ui.graphics.drawscope.clipPath`,
  MetalMaterial:84 — it is a DrawScope extension, not a member).
  Fixed in f3d0464f. Line numbers matched the remote exactly, so these
  were real errors in the current copy.

- 2026-11-16: the chapter-card change imported `RectangleShape` from
  `androidx.compose.foundation.shape`, but it lives in
  `androidx.compose.ui.graphics` (`RoundedCornerShape` is the one in
  `foundation.shape`), so both chapter item files failed with
  "Unresolved reference 'RectangleShape'". Fixed by moving the import
  to `androidx.compose.ui.graphics.RectangleShape` in
  MangaChapterListItem.kt and MissingChapterCountListItem.kt (ad14c2d0).


- 2026-11-14: the Explore capsule called `stringResource(...)` for the
  two `contentDescription`s directly inside the `LaunchedEffect` that
  reports the actions — but `stringResource` is @Composable and a
  `LaunchedEffect` block is a suspend lambda, not a composable context,
  so both calls failed with "@Composable invocations can only happen
  from the context of a @Composable function". Fixed by hoisting the
  two strings out of the effect (read in the composable body, like the
  Library page does with its capsule descriptions) and using the plain
  `String` values inside.


- 2026-11-14: the Explore capsule used `MaterialSymbols.Rounded.Search`,
  `RoundedFilled.ViewCompact`, `RoundedFilled.ViewModule` and
  `AutoMirroredRoundedFilled.ViewList`, but BrowseSourceScreen.kt did
  not import those icon files — Tychii's icon library needs a direct
  import per icon (the `MaterialSymbols` base import alone does not
  resolve them), so all four were "Unresolved reference", cascading
  into "@Composable invocations" errors on the `stringResource` calls
  in the same `CapsuleAction` calls. Fixed by adding the four imports
  (the same ones LibraryContent.kt already has).


- 2026-11-13: the waterfall `items` calls in UpdatesUiItem.kt and
  HistoryScreen.kt passed `span = { _, item -> ... }`, but the
  `List<T>` overload's `span` takes only the item, not (index,
  item) — the (index, item) form belongs to the index-aware overload,
  whose `itemContent` also takes both. With no matching overload the
  whole `items` call failed to resolve, cascading into "Unresolved
  reference 'it'" / "Cannot infer type" / "@Composable invocations"
  errors. Fixed by using `span = { item -> ... }`.
- 2026-11-13: LibraryCompactGrid.kt and LibraryComfortableGrid.kt
  still imported the regular grid's `items`
  (`lazy.grid.items`), but their content lambda is now a
  `LazyStaggeredGridScope`, so the List-based staggered `items`
  extension never resolved — the compiler fell back to the scope's
  member `items(count: Int, ...)` ("No value passed for parameter
  'count'", and the item lambda's parameter became `Int`). Fixed by
  importing `lazy.staggeredgrid.items` in both files.

## Waterfall layout on Library, Updates and History (2026-11-13)

The three main pages are now waterfall (masonry) layouts, and the
only things floating above the content are the title capsule, the
bottom bar and the floating action capsule (search / sort / display /
filter) — no other overlay.

- Library: the grid is now a `LazyVerticalStaggeredGrid`
  (`LazyLibraryGrid`), so the PiliPlus-style cards (cover + a 1- or
  2-line title) flow into two uneven columns instead of lining up in
  uniform rows. The list display mode is untouched (still a list).
  The Browse source grids still use the regular grid.
- Updates: the list is now a 2-column staggered grid of cards.
  Each update is a surface card with the manga cover on top (top
  corners rounded) and the manga title + chapter below; the unread
  dot / bookmark sit on the cover, the download indicator at the
  cover's bottom-end. Date headers span the full width. Selection is
  a primary outline around the card with a dimmed cover.
- History: the list is now a 2-column staggered grid of cards. Each
  entry is a surface card with the cover on top and the title +
  chapter/time below; the favourite / delete actions sit in a small
  row under the text. Date headers span the full width.

## Explore chips: breathing room from the toolbar and divider (2026-11-13)

On the per-source Explore page the Popular / Latest / Filter chips sat
flush against the toolbar above and the divider below, with no gap
either side. The chips row now has 8.dp of vertical padding, so it
breathes on both sides.

## Floating action capsule: clear of the right edge (2026-11-13)

The floating action capsule (search / sort / display / filter) sat
12.dp from the right edge, hugging the screen edge. Its end padding
is now 20.dp, so it floats clear of the edge.

## Library and Updates: no containers above the content (2026-11-13)

The containers above the Library and Updates pages are gone, leaving
only the title bar (the floating capsule). On the Library this was the
category tabs row (`LibraryTabs`): the pager still swipes between
categories, and the capsule title now always names the category being
viewed (with its manga count), so there is still a way to tell which
one it is — `getToolbarTitle` no longer falls back to the default
title or the whole-library count when tabs were shown. On Updates
this was the "last updated" header (`updatesLastUpdatedItem` /
`UpdatesLastUpdatedHeader`, an italic line at the top of the list and
in the empty state). The now-unused `LibraryTabs.kt`,
`showPageTabs` / `getItemCountForCategory` plumbing and the
`lastUpdated` parameter are removed. The `showCategoryTabs` setting
itself is left in place (it no longer drives anything on the Library
page).

## Theme mode TabRow: match the card background (2026-11-13)

The theme mode TabRow （系統 / 淺色 / 深色） in Settings → Appearance
still read as a mismatched band: its default row background is
`surface` (pure black in dark mode, #F7F7F7 in light), while the card
it sits in is `surfaceContainer`. The row now takes
`surfaceContainer` so it blends into the card, and the selected tab
takes `surfaceContainerHighest` (one shade up: #2D2D2D in dark,
#E8E8E8 in light) so it still stands out. The earlier
`itemSpacing = 0.dp` fix (one segmented control, no black gaps) is
unchanged.

## Extension status pill (2026-11-13)

`ExtensionPill` (the status capsule in the Extensions list and on the
extension details page — install state, obsolete, content warning,
private installer) is now a true pill: fully rounded ends
(`CircleShape`) instead of the 4.dp rounded rectangle, a slightly
stronger tinted background of the status color (0.15 alpha), a
medium-weight label, and a little more breathing room (8.dp / 3.dp
padding). The status colors themselves are unchanged.

## Tab lists: no scrollbar thumb, selected icon, edge-to-edge (2026-11-13)

Three small fixes in one round. The tab lists (Library grid and
list, Updates, History, Browse menu, More) no longer show the
right-edge drag indicator: they use plain `LazyColumn` /
`LazyVerticalGrid` instead of `FastScrollLazyColumn` /
`FastScrollLazyVerticalGrid` / `ScrollbarLazyColumn`. The shared
components themselves are untouched, so settings and other screens
keep their scrollbars; only the five tabs lose the thumb (it sat
over the covers next to the floating capsule).

In liquid-glass mode the selected tab's icon vanished: the sliding
indicator pill is a second glass layer, and it was drawn over the
bar's Row, so it painted the blurred page over the icon. The bar
is now three layers — glass background Row, indicator pill, then
the tab content in its own Row on top — so the icons stay visible
above the pill. The content Row mirrors the background Row's size
and motion (same height, padding, rubber-band offset and press
scale) so the icons sit exactly where they did.

Edge-to-edge: the themes painted the navigation bar solid black
(`@color/amoled_surface`, `#000000`) while the app background is
`#1A1C1E`, so a dark band showed between the gesture bar and the
app. `android:navigationBarColor` is now `@android:color/transparent`
in both `values/themes.xml` (Base.Theme.Tachiyomi) and
`values-v27/themes.xml` (Base.V27.Tachiyomi), matching the
status bar and MainActivity's `enableEdgeToEdge`.

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

- 2026-11-13 (9e1ea10a): the PiliPlus card restyle removed the `Color`
  import along with the deleted title overlay, but `selectedOutline`
  (line ~301) still takes a `Color` parameter — the import is restored.
  `Brush` and `Shadow` are still unused and stay removed.

Local build errors reported by the user after each push, fixed the
same way (read the file, check the Miuix/library source before
asserting an API is unavailable, scan tree-wide for the same error
class, push, verify HEAD). Compilation is unverified in the cloud
(the Gradle daemon fails to start there); the user builds locally
from the GitHub ZIP.

- 2026-11-15 (4c7793e9): the dialog migration added a `Row` using
  `Modifier.fillMaxWidth()` to `DeleteLibraryMangaDialog`, but the
  file never used `Modifier` before, so the import was missing —
  `Unresolved reference 'Modifier'` at line 58. Import added; the
  other two files in the same batch already imported it.
- 2026-11-15 (c67e1190): the selection-pill change removed the
  `AppBar` import from `LibraryToolbar` after checking only for
  `AppBar(` call sites — but the regular toolbar still uses the
  nested `AppBar.Action` / `AppBar.OverflowAction` types (lines
  127/133/137/141), so all four errored. Import restored. Same
  lesson as the `Color` case (9e1ea10a): grep the bare type name when
  cleaning imports.

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
- 2026-11-13 — `BrowseSectionScreen` held `val navigateUp =
  { navigator.pop() }`; `pop()` returns Boolean, so the lambda
  inferred `() -> Boolean` and the three section call sites
  rejected it (expected `() -> Unit`). The val is now typed
  explicitly `() -> Unit`, which coerces the Boolean result.
- 2026-11-13 — the floating capsule's `showSearchIcon` was folded
  into SearchToolbar's magnifier branch as
  `searchQuery == null && showSearchIcon`, which broke the smart
  cast: the next branch (`searchQuery.isNotEmpty()`) no longer knew
  `searchQuery` was non-null, so the compiler rejected it as
  UNSAFE_CALL. The branch is back to the bare null check, with
  `showSearchIcon` gating the magnifier inside it (219b4790).

## Build

JDK 21, Android SDK API 37, then:

```sh
./gradlew :app:assembleDebug
```

Not verified in this environment: the local Gradle daemon fails to start
here (dispatch/Broken pipe before compilation), the same failure seen
with the earlier cloud build. Build on a local machine to verify.
