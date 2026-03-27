# POS APP Release Workflow

## Required checks for every POS APP update

1. Record any new device constraints in `docs/field_devices.md`.
2. Review RWD against the primary tablet baseline:
   - width: 21.5 cm
   - height: 13.5 cm
   - expected orientation: landscape
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

## Interaction and status responsiveness requirements

- Any tappable control that changes state must reflect that state change immediately in the same visible panel.
- Toggles, connect or disconnect buttons, scan or stop buttons, and printer selection controls must not require leaving and re-entering the screen to show the new state.
- Status messages and error messages must update in the same interaction flow where the action happened, so the operator is not misled by stale UI.
- If an async action is in progress, the UI must show a clear in-progress state such as loading, disabled actions, or updated helper text before the action completes.
- When a device list or connection state changes, the visible list and current selection summary must re-render immediately.

## Execution discipline for follow-up work

- When a user gives a specific implementation instruction, complete that instruction first before returning to unrelated cleanup or extra polish.
- If a nearby issue is discovered while implementing the requested work, note it, but do not switch focus unless it directly blocks the requested task.
- After the requested task is complete, any additional fixes should be clearly labeled as follow-up work rather than mixed into the primary request without notice.
