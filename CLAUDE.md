# iOS2DroidKeyboard

Android keyboard (IME) that types like an iPhone keyboard. Goal: someone who switches between an iPhone and an Android phone every day should get the same muscle memory on both — same key sizes, spacing, touch targets, popups and gestures.

- App name: **iOS2DroidKeyBoard**
- Package: `io.github.splashieee7.ios2droidkeyboard`
- Release APK name: `iOS2DroidKeyBoard-<version>.apk`

Standalone open-source project. It is not tied to any company, so don't add company names, branding or links.

Must work on de-Googled phones (e.g. GrapheneOS) and ship as public releases (GitHub Releases, later F-Droid). So: **no Google Play Services, no Firebase, no analytics, no network.**

## Hard rules

- **No `INTERNET` permission.** Ever. The manifest must not request it, and no dependency may pull it in. Check the merged manifest after adding any library.
- No Google Play Services / GMS / Firebase / Crashlytics.
- Nothing typed leaves the device or gets logged. No `Log.d` of key input, even in debug builds.
- Don't use Apple assets: no SF Pro font, no Apple emoji images, no sounds ripped from iOS. Recreate the *feel* with our own assets; use the system emoji font.
- Must build fully from source with open tooling (F-Droid requirement): no proprietary binary blobs.

## Stack

- Kotlin, Jetpack Compose for the keyboard UI, Gradle Kotlin DSL with a version catalog
- `InputMethodService` hosting a `ComposeView` (the IME window needs `ViewTreeLifecycleOwner`, `ViewTreeViewModelStoreOwner` and `ViewTreeSavedStateRegistryOwner` set on the decor view or Compose will crash)
- minSdk 26, target/compile the latest stable SDK
- Settings stored locally with DataStore
- GitHub Actions: build + lint + unit tests on every push; signed release APK on tags

## What "feels like iOS" means (priority order)

1. **Geometry** — key width/height, gaps, corner radius, row offsets (ASDF row indented half a key), bottom row layout. All sizes live in one `KeyboardMetrics` object in dp, calibrated from real iPhone screenshots in `/reference`. No magic numbers scattered around.
2. **Touch targets** — each key's hit area extends to cover the gaps around it, so there are no dead zones. A touch always goes to the nearest key. This is the biggest usability difference vs stock Android.
3. **Key press popup** — the enlarged letter bubble above the key on press (letter keys only), long-press for accented characters.
4. **Space-bar cursor drag** — long-press space → keys blank out, dragging moves the cursor.
5. **iOS typing behaviors** — auto-capitalize after `. ` and at field start; double-tap shift = caps lock; double-space inserts `. `; after typing a punctuation mark from the 123 layer, switch back to letters; backspace hold accelerates from chars to whole words.
6. **Feel** — haptic tick + key click sound (respect system settings), light and dark themes matching iOS colors.
7. **Emoji panel** — categories, recents, search later. System emoji font.
8. **Swipe/glide typing** — separate phase, needs a word list + path-matching. Don't start until 1–7 are solid.

Autocorrect / suggestion bar is out of scope for v1.

## Project layout (target)

```
app/src/main/java/.../
  ime/        InputMethodService, input connection helpers
  layout/     Key, Row, KeyboardLayout models; QWERTY, 123, #+= layers
  metrics/    KeyboardMetrics (all sizes), hit-testing
  ui/         Compose keyboard, key, popup, emoji panel
  gestures/   space-drag, backspace repeat, glide (later)
  settings/   settings activity + DataStore
reference/    iPhone screenshots + measured metrics notes
```

## Conventions

- Keep hit-testing and layout math pure Kotlin (no Android deps) so it's unit-testable.
- Every behavior in "iOS typing behaviors" gets a unit test.
- Small commits, one feature at a time. Don't add a library without saying why.
- Dev machine is Windows / PowerShell — any commands you give me should work there (`.\gradlew.bat`, not `./gradlew`).
