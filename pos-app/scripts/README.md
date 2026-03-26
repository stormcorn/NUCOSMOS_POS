# POS Build Scripts

Primary tablet baseline for RWD validation:

- 21.5 cm x 13.5 cm
- See `../docs/field_devices.md`

## Build release APK with a custom name

Run this from the `pos-app` directory:

```powershell
.\scripts\build_release_apk.ps1
```

The script runs a text audit first and stops the build if it finds suspicious
UI literals such as `???` or replacement characters.

Before packaging a POS APP update, also review the release checklist in
`../docs/release_workflow.md`.

This keeps Flutter's default output:

```text
build\app\outputs\flutter-apk\app-release.apk
```

And also creates a descriptive copy such as:

```text
build\app\outputs\flutter-apk\nucosmos-pos-fix-make-quick-receive-form-fully-scrollable-ab2d3b6.apk
```

You can also override the label and revision manually:

```powershell
.\scripts\build_release_apk.ps1 -Label "quick receive scroll fix" -Revision "ab2d3b6"
```
