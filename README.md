# Tychii

A personal fork of [Mihon](https://github.com/mihonapp/mihon), restyled with
the [Miuix](https://github.com/compose-miuix-ui/miuix) component library
(HyperOS visual language) and the Neko theme. It reads manga, webtoons and
comics on Android, same as Mihon — only the look is different.

## What changed

- **Floating liquid-glass navigation bar** on phones: five tabs in a
  detached capsule, blur + vibrancy + lens refraction on API 32+,
  translucent fallback below. Three bar styles in Settings → Appearance
  （栏样式）: opaque, translucent, liquid glass.
- **Centered title capsule** at the top of each main tab instead of
  Mihon's top-left toolbar title. It follows the active library category
  （书架 · 分类名） and hides while searching or selecting.
- **Miuix everywhere except the reader**: settings, manga detail,
  browse / sources / extensions / migration, updates, history, download
  queue, categories, stats, upcoming, dialogs and sheets. The reader is
  untouched.
- **Neko style**: primary `#FF124A` light / `#FCFC98` dark, M PLUS
  Rounded 1c font throughout, 12.dp cover radius with a theme-following
  outline. Manga detail pages take a quiet color from their cover.
- **Taiji launcher icon**: a black-and-white yin-yang, the same in day
  and night mode.

See [TYCHII-MIUIX.md](TYCHII-MIUIX.md) for the full notes: what changed
file by file, what was deliberately not changed, and the build fixes so
far.

## Build

Requires Android 8.0 or higher (minSdk 26). JDK 21, Android SDK API 37:

```sh
./gradlew :app:assembleDebug
```

Current version: **v0.1.2**. Package name is unchanged (`app.mihon`).

## License

Apache License 2.0 — see [LICENSE](LICENSE). Mihon's code and assets
remain under their original licence; the Miuix library is under its own
licence. See the licences screen in the app for third-party licences.
