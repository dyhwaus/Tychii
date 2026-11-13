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

The top got the same treatment (2026-11-13): the first row was
being cut off by the title capsule band. Each list now also adds a
shared `TopListGap` (32.dp) above its first item — 64.dp toolbar
band + 32.dp gap = 96.dp in total, mirroring the bottom. The
Library had an extra bug here: it built a fresh `PaddingValues`
with only the bottom, dropping the Scaffold's own top padding (the
64.dp band), so its first row slid under the band; it now adds to
the Scaffold's padding with the `plus` operator like the other
lists.

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
