# POS APP Release Workflow

## Required checks for every POS APP update

1. Record any new device constraints in `docs/field_devices.md`.
2. Review RWD against the primary tablet baseline: 21.5 cm x 13.5 cm.
3. Run text audit to block suspicious UI literals such as `???` or replacement characters.
4. Run `flutter test`.
5. Build release APK with `scripts/build_release_apk.ps1`.
6. Report all of the following together:
   - commit hash
   - descriptive APK file name
   - APK path

## Tablet-specific RWD expectations

- No critical action may be reachable only on large desktop heights.
- Nested scrolling should be avoided unless the inner scroll region is intentional and tested.
- Forms must keep labels, helper text, and submit buttons reachable by touch scrolling.
- Product grids on compact tablet layouts should not depend on hard-coded heights.
