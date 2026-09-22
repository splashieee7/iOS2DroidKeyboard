# iOS2DroidKeyBoard

An Android keyboard (IME) that types like an iPhone keyboard — same key sizes, spacing,
touch targets, popups and gestures, so switching between an iPhone and an Android phone
doesn't cost you your muscle memory.

Runs on de-Googled phones. **No network, no Google Play Services, no analytics.** The app
does not request the `INTERNET` permission and a CI check fails the build if any dependency
tries to add it.

## Status

**v1.0** — usable as a daily keyboard.

- [x] QWERTY, 123 and #+= layers, calibrated from reference screenshots
- [x] Gap-free hit-testing — no dead zones between keys
- [x] Key-press popup bubble and long-press accent menus
- [x] iOS typing behaviour: three-state shift, double-space period, auto-capitalise,
      layer auto-return after punctuation
- [x] Hold-to-repeat backspace that accelerates to whole words
- [x] Space-bar cursor drag
- [x] Haptics, click sound, light/dark themes
- [ ] Emoji panel — the key is there but does nothing yet
- [ ] Glide typing
- [ ] Autocorrect and suggestion bar

## Installing

Grab `iOS2DroidKeyBoard-<version>.apk` from
[Releases](https://github.com/splashieee7/iOS2DroidKeyboard/releases), sideload it, then
open the app, enable the keyboard in system settings, and switch to it.

## Building

Requires JDK 21 and the Android SDK (compileSdk 37, minSdk 26).

```powershell
.\gradlew.bat assembleDebug
```

The APK lands in `app\build\outputs\apk\debug\app-debug.apk`.

`assembleRelease` produces `iOS2DroidKeyBoard-<version>.apk`. It is unsigned unless a
`keystore.properties` is present, so building from source always works — you just get an
unsigned APK, which is what F-Droid and reproducibility checks need.

### Installing on a phone

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Then open the app, enable the keyboard in system settings, and switch to it.

## Geometry

Key sizes are not guesses. They are measured from real iPhone screenshots and stored as a
share of screen width, which is what makes them portable to Android screens of other sizes —
including phones with a display-size override, where dp-based layouts silently resize.
See [`iphone-metrics.md`](iphone-metrics.md) and the `reference/` folder.

## Privacy

No `INTERNET` permission, no permissions at all in fact. Nothing you type is logged or
leaves the device. The `checkNoInternetPermission` Gradle task scans every merged manifest
and fails the build if a dependency adds the permission, so this is enforced rather than
promised.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
