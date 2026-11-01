# iOS client

Native Swift + SwiftUI, iOS 17+. Bundle ID `net.interlecto.proho`
(`net.interlecto.proho.debug`, shown as "ProHo (dev)", for Debug builds, so
both can be installed side by side like on Android).

Status: **skeleton.** `ProHoKit` builds and its tests pass on Windows (Swift
6.4.2). The SwiftUI app target was written without Xcode and has not been
compiled yet. CI's macOS job (`.github/workflows/ci.yml`, job `ios`) and the
first Mac session (checklist below) are the first real compile of the app
target.

## Layout

| Path                  | What it is                                                         | Builds on            |
| --------------------- | ------------------------------------------------------------------ | -------------------- |
| `ProHoKit/`           | Swift package: API models, `APIClient`, error→message-key mapping, unit tests. No UIKit/SwiftUI. | macOS, Windows, Linux |
| `ProHo/`              | The SwiftUI app: `App/` (entry point, session), `Screens/`, `Theme/`, `Resources/` (strings, icon) | macOS (Xcode) only   |
| `project.yml`         | [XcodeGen](https://github.com/yonaskolb/XcodeGen) spec. The `.xcodeproj` is generated from it, not committed | macOS               |
| `Config/*.xcconfig`   | Bundle ID, display name and API base URL per configuration         |                      |

Rules of thumb:

- **Put logic in `ProHoKit`.** Anything that doesn't draw pixels (API calls,
  DTOs, validation, error mapping, formatting, state machines as plain
  structs) goes in the package, where it can be written *and tested* on
  Windows. The app target should stay thin.
- **Don't commit `ProHo.xcodeproj`.** Add files to the folders and re-run
  `xcodegen generate`. Project settings go in `project.yml` or the xcconfigs.
- **Strings:** `ProHo/Resources/{es,en}.lproj/Localizable.strings` use the
  same keys as Android's `strings.xml` (they were generated from it). Spanish
  is the development language. Pass keys to SwiftUI as literals or as
  `LocalizedStringKey`; a plain `String` is shown as-is, not translated.
- **API surface:** `APIClient` mirrors `android/.../net/ApiClient.kt`. Add an
  endpoint to both, and to `api/openapi/openapi.yaml`.

## Working on Windows

Only `ProHoKit` can be built and tested here. Verified with Swift 6.4.2 and
VS 2022 Build Tools 17.14 (MSVC 14.44, Windows SDK 10.0.22621).

One-time setup:

```powershell
winget install --id Swift.Toolchain -e
winget install --id Microsoft.VisualStudio.2022.BuildTools -e --override "--passive --wait --norestart --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64 --add Microsoft.VisualStudio.Component.Windows11SDK.22621"
```

Then open a **new** terminal (the Swift installer sets `SDKROOT` and `PATH`
for new sessions only) that has the MSVC environment loaded: *x64 Native
Tools Command Prompt for VS 2022* or *Developer PowerShell for VS 2022* from
the Start menu. In a plain terminal, `swift build` fails with "could not
find CLI tool `link`".

```bat
cd ios\ProHoKit
swift build
swift test
```

Expected noise that can be ignored:

- `warning: reference to type 'wchar_t' broken by a context change` comes
  from the toolchain's own `WinSDK` module, not from ProHo code.
- `warning: unable to create symbolic link at ...\.build\debug`: Windows
  only lets normal users create symlinks with Developer Mode enabled
  (Settings → System → For developers). It's harmless either way.

VS Code with the official *Swift* extension (swiftlang.swift-vscode) gives
completion and test running for `ProHoKit`. For files under `ProHo/`, expect
"no such module SwiftUI": those only type-check on a Mac.

## Working on the Mac

One-time setup:

```sh
xcode-select -p            # Xcode 16 or newer, with an iOS 17+ simulator
brew install xcodegen
cd ios
cp Config/Local.xcconfig.example Config/Local.xcconfig   # set DEVELOPMENT_TEAM
xcodegen generate
open ProHo.xcodeproj
```

Running against a local backend (see `docs/architecture/client-screens.md`):

- **Simulator:** shares the Mac's network, so the Debug default
  `http://127.0.0.1:8080` reaches a backend running on the same Mac.
- **Physical iPhone:** set `PROHO_API_BASE = http:/$()/<mac-name>.local:8080`
  in `Config/Local.xcconfig`. Plain HTTP to `.local` hosts is allowed by
  `NSAllowsLocalNetworking`. iOS will ask for local-network permission the
  first time.
- If the backend runs on the Windows machine instead, use its LAN IP or
  hostname and make sure the firewall allows port 8080.

Command-line equivalents (what CI runs):

```sh
swift test --package-path ProHoKit
xcodebuild build -project ProHo.xcodeproj -scheme ProHo \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

### First Mac session: checklist

Items that could not be verified without Xcode, in order:

1. `xcodegen generate` succeeds. If it rejects the scheme's
   `package: ProHoKit/ProHoKitTests` test entry (older XcodeGen), remove that
   entry and keep running the package tests with `swift test`.
2. `swift test --package-path ProHoKit` passes on macOS.
3. The app builds for the simulator with no errors. Fix and commit any
   warnings that come up.
4. It launches with the monogram icon, an ink-navy bar showing "ProHo" plus
   the tagline, and Spanish text on a Spanish-language simulator (English on
   an English one).
5. The demo slice runs end to end against a local backend: register, land on
   "Sus comunidades", create a community, see it listed, pull to refresh,
   log out. Check that a duplicate key shows the translated error.
6. Dark mode: the gold accent switches to the lighter shade.
7. On a physical device with `Local.xcconfig`: signing works and the app
   reaches the Mac's backend over `.local`.
8. Add the `ios` job to the required checks on `master` once it's green.

## Development plan

The iOS client follows the same screen set as Android
(`docs/architecture/client-screens.md` §2) and the phases in `ROADMAP.md`.
Each step says where the work happens: **W** = writable and testable on
Windows (`ProHoKit`), **M** = needs a Mac session to build, run or polish.

### Step 0: Skeleton (this branch, `develop/ios-shell`)

- ✅ **W** `ProHoKit`: models, `APIClient` (accounts, login, me, logout,
  password reset, communities), error mapping, unit tests against a stub
  transport.
- ✅ **W** App target: entry point, session model, auth screen, communities
  list, create-community sheet, brand theme, es/en strings, app icon.
- ✅ **W** XcodeGen spec, xcconfigs, Info.plist, CI jobs (`ios-kit-linux`,
  `ios`).
- ⬜ **M** Work through the first-session checklist above. This is the exit
  criterion for merging to `master`.

### Step 1: Parity with the Android demo slice

Already coded in step 0 and verified by checklist item 5. After that the
demo slice from `client-screens.md` §4 runs on all three clients.

### Step 2: Real session handling

- **W** `TokenStore` protocol in `ProHoKit` with an in-memory implementation
  for tests. Restore the session on launch by calling `GET /me` and dropping
  the token on `401`.
- **M** Keychain-backed `TokenStore` in the app target
  (`kSecClassGenericPassword`, `AfterFirstUnlockThisDeviceOnly`).
- **W/M** Forgot-password flow: request screen and confirm screen (token +
  new password). The API already exists.
- Android currently keeps the token in memory only. Do the same work there
  so the clients stay in step.

### Step 3: Community home (Phase 1 screens)

- **W** `APIClient` methods for buildings, units, people and unit
  relationships (`/communities/{id}/…`, already in the backend), with tests.
  Add them to `openapi.yaml` at the same time.
- **M** Community switcher, remembering the last community used per account.
- **M** A `TabView` home: *Inicio* (placeholder until billing exists),
  *Directorio* (people), *Unidades* (list → detail with its relationships),
  *Perfil* (profile and logout).
- **M** Role gating plus trimmed admin forms (create unit, create person,
  assign relationship, showing `owner_share_exceeds_100` properly), same
  scope as Android.
- **W** Decision point: once `openapi.yaml` covers these endpoints, consider
  generating the `ProHoKit` models with Apple's `swift-openapi-generator`
  instead of hand-writing them, and doing the same for Android/web. This
  becomes worth it as the contract grows.

### Step 4: Distribution

Answers `ROADMAP.md` open question 5:

- Apple Developer Program membership for Interlecto, App Store Connect
  record for `net.interlecto.proho`.
- TestFlight builds from CI: signing via App Store Connect API key in CI
  secrets (fastlane `match`, or Xcode Cloud). As with Android, no signing
  material in the repo.
- Privacy manifest (`PrivacyInfo.xcprivacy`) and App Store privacy labels
  before the first external TestFlight.

### Step 5: Later phases, following the backend

In the order the backend delivers them, keeping parity with Android:

- Account statements, balances, payment history (Phase 2/3). Decode and show
  money as `Decimal` with `FormatStyle.Currency`, never `Double`, and agree
  the JSON representation of amounts with the backend first.
- Announcements and direct messages (communications module).
- Push notifications via APNs: device-token registration endpoint in the
  backend, `UNUserNotificationCenter` in the app.
- Offline reading of synced news and messages (`ROADMAP.md` acceptance
  criteria), likely SwiftData, with the sync rules shared with Android.
- Accessibility pass (Dynamic Type, VoiceOver labels) and iPad layout
  (`NavigationSplitView`) once the screen set settles.
