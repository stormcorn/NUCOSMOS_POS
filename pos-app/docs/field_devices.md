# Field Device Profiles

## Primary tablet baseline

- Purpose: POS APP RWD and touch-flow validation
- Physical size:
  - Width: 21.5 cm
  - Height: 13.5 cm
- Expected orientation: landscape
- Notes:
  - Treat this as the primary tablet baseline for layout checks.
  - Quick receive, checkout, login, and long forms must remain fully reachable by touch scrolling on this device class.
  - When a UI change affects vertical layout, verify that bottom actions and helper text remain reachable without clipping.

## Validation focus

- Quick receive:
  - Material
  - Manufactured item
  - Packaging
- Login screen
- Checkout panel
- Product grid on compact tablet widths
- Printer settings panel:
  - Toggle state must refresh immediately in place.
  - Scan, stop, connect, disconnect, and selection changes must update visible status without requiring a screen switch.
  - Device diagnostics, connection summaries, and error banners must always reflect the latest action in the same panel.
