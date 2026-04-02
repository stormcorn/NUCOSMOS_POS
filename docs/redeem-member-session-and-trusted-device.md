# Redeem Member Session And Trusted Device

## Goal

The public redeem page at `https://nucosmos.io/redeem/` supports:

- SMS-based member login
- session-based auto login
- trusted-device login when the session cookie is missing or expired

## Rules

- A verified member can log in with phone number + SMS verification.
- After a successful login, the backend issues an HttpOnly cookie named `nucosmos_member_session`.
- The session TTL is 90 days.
- If the user checks "remember this device", the browser sends a device token and the backend stores a trusted-device record.
- Trusted devices stay valid for 365 days unless revoked or expired.
- If the session cookie is missing but the device token is still trusted, the backend restores a fresh session automatically.
- If the session cookie is invalid and the device token is not trusted, the page must ask the user to verify again.

## Frontend Contract

- The redeem page must send these headers on public member requests:
  - `X-Nucosmos-Device-Token`
  - `X-Nucosmos-Device-Label`
- The device token is stored in browser localStorage.
- The redeem page must keep the SMS login form visible whenever the member is not authenticated.
- When the member is authenticated, the page should show the member card and hide the login form.

## Backend Tables

- `receipt_member_sessions`
- `receipt_member_trusted_devices`

## Notes

- Logging out clears the active session cookie.
- Logging out also revokes the trusted device when the device token is present.
- This feature is for the public redeem flow only and does not change the POS APK.
