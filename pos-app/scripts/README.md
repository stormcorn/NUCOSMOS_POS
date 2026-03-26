# POS Build Scripts

## Build release APK with a custom name

Run this from the `pos-app` directory:

```powershell
.\scripts\build_release_apk.ps1
```

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
