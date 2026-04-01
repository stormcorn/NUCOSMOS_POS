# Receipt Redeem MVP

## Purpose

Turn each printed receipt into a safe public redeem entry on `nucosmos.io`, so customers can:

- scan a QR code or open a redeem URL from the receipt
- verify the receipt is real
- confirm whether it has already been claimed
- complete a first-stage redeem action on the website
- bind the receipt to a member profile using name and phone number

This is the foundation for later member, points, coupon, or lucky-draw features.

## Public Entry

- Public page route: `/redeem/:token`
- Public API:
  - `GET /api/v1/public/redeem/{token}`
  - `GET /api/v1/public/redeem/search?code=<claimCode>`
  - `POST /api/v1/public/redeem/{token}/claim`

## Security Rules

- Do not expose raw order IDs as the redeem credential.
- Each order uses a unique, non-guessable `public_token`.
- Each order also has a short `claim_code` for manual input fallback.
- The public website may show receipt status, but only the token/code determines lookup.

## Backend Data

Table: `receipt_redemptions`

- one row per order
- `order_id` unique foreign key
- `public_token` unique
- `claim_code` unique
- `claimed_at` nullable
- `claimed_member_id` nullable

Table: `receipt_members`

- stores the public member profile collected from the redeem page
- unique `phone_number`
- reusable for future points, coupons, and campaign eligibility
- tracks `point_balance` and `total_claims`

Table: `receipt_coupons`

- stores reward coupons generated from redeem milestones
- linked to both member and source redemption
- first MVP issues a coupon automatically when points reach a threshold

## Eligibility Rules

An order is redeemable only when:

- order is not `VOIDED`
- payment status is one of:
  - `PAID`
  - `PARTIALLY_REFUNDED`
  - `REFUNDED`

An unpaid order must not be redeemable.

## Receipt Requirements

Both thermal receipts and Android system-print receipts should include:

- redeem code
- redeem URL
- QR code when the printer path supports it safely

If a printer path cannot safely render the QR content, it must still print the URL/code text.

## POS / Web Responsibilities

### POS

- receive `redeemCode` and `redeemUrl` from order APIs
- print them on the receipt
- never generate redeem tokens locally

### Web Public Page

- allow token-based lookup from QR URL
- allow manual redeem-code lookup
- require member name and phone number before a receipt can be claimed
- show current member points and any coupon issued from this redeem
- show clear status:
  - ready to redeem
  - already redeemed
  - not eligible

## Reward Rules (MVP v1)

- each successful redeem adds `1` point
- when a member reaches every `5` points, the system automatically issues one `NT$20` coupon
- redeem remains one-time only per receipt

## Deployment Note

Backend uses:

- `PUBLIC_WEB_BASE_URL`

Production value should be:

- `https://nucosmos.io`

If omitted, backend falls back to that same default.

## Public Routing

- Public redeem page must not expose `/erp` to customers.
- Public route `/redeem/` is served by backend static content.
- Apache should proxy:
  - `/redeem/` -> `http://127.0.0.1:8081/redeem/`
- ERP admin remains under:
  - `/erp/`
