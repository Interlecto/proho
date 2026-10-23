# Development workflow

## Branches

- `master` holds reviewed work that builds.
- Work is done on `develop/<topic>` branches cut from `master`, which merge
  straight back into `master` (e.g. `develop/foundation`,
  `develop/android-shell`, `develop/ledger-core`).
- Large developments may use their own sub-branches, but only as laid out in
  an approved plan.

## Android signing

Debug builds are signed with a shared key committed at
`android/keystore/debug.keystore` (alias `androiddebugkey`, store and key
password `android`). Every machine and CI produces identically signed debug
APKs, so a test device accepts a build from any of them as an update of the
same app, without uninstalling it and losing its data.

Debug builds use the application ID `net.interlecto.proho.debug` and the label
"ProHo (dev)", so they can be installed next to a release build.

The release key must never be committed. It will be provided to the build
through environment variables or CI secrets.

Debug certificate: `CN=ProHo Debug, O=Interlecto, C=CO`, SHA-256
`03:C3:FE:52:FF:FA:95:3D:97:22:1C:56:E4:F6:21:FF:D4:1C:3E:51:ED:11:08:CE:5B:A9:8A:35:92:E0:F8:30`
(valid until 2056). Check an APK with `apksigner verify --print-certs`.
