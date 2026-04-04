# 2F Space Booking MVP

## Goal

Add a rentable `2F` event space booking flow for NUCOSMOS.

The first release should support:

- public space introduction page on `nucosmos.io`
- public availability calendar
- booking request form
- admin review and confirmation flow
- internal calendar / blockout management
- hourly pricing based on a fixed rule

Initial business rule:

- space: `2F Event Space`
- price: `NT$1,000 / hour`

## Product Positioning

The recommended operating model is:

- `nucosmos.io` is the primary booking system
- external venue platforms are used for exposure and lead generation
- final availability must be controlled by one internal calendar

This avoids double-booking across:

- official website
- phone / manual reservations
- external venue platforms

## MVP Scope

### Included

- one rentable space resource
- hourly booking slots
- public booking request page
- public availability calendar
- admin booking calendar
- admin approval / rejection
- admin manual booking creation
- admin blockout management
- booking source tracking
- deposit and payment fields reserved in the data model

### Not included in MVP

- instant booking without review
- online payment gateway
- Google Calendar sync
- automatic sync with external rental platforms
- member self-service booking history
- self-service reschedule / cancellation

## Booking Flow

### Public customer flow

1. Customer opens `nucosmos.io/space`
2. Customer browses space details, images, rules, and pricing
3. Customer checks available time slots on the calendar
4. Customer selects date and start/end time
5. Customer fills:
   - name
   - phone
   - email
   - purpose
   - attendee count
   - note
6. Customer submits a booking request
7. Admin reviews the request
8. Admin accepts or rejects the request
9. Accepted booking becomes a confirmed event on the calendar

### Admin flow

1. Open admin booking calendar
2. Review pending booking requests
3. Accept / reject / edit requests
4. Create manual bookings from phone / platform inquiries
5. Create blockout periods for:
   - internal use
   - maintenance
   - special events
   - unavailable periods
6. Record deposit / payment status manually

## Booking Rules

These should be configurable in the data model even if the first release hardcodes them in UI.

- hourly rate: `1000`
- currency: `TWD`
- minimum booking duration: `1 hour` or `2 hours` depending on final business decision
- booking unit: `1 hour`
- booking buffer: suggested `30 minutes` before/after each booking
- max attendee count
- open hours by weekday
- cancellation rules
- deposit rules
- overtime rules

Recommended first release defaults:

- open hours: `10:00 - 22:00`
- booking unit: `1 hour`
- minimum booking duration: `2 hours`
- buffer after booking: `30 minutes`

## Data Model

### `space_resources`

Space master data.

Suggested fields:

- `id`
- `code`
- `name`
- `slug`
- `description`
- `capacity`
- `location_label`
- `active`
- `created_at`
- `updated_at`

First record:

- `code = SPACE_2F`
- `name = NUCOSMOS 2F Event Space`
- `slug = 2f-event-space`

### `space_booking_policies`

Booking policy per space.

Suggested fields:

- `id`
- `space_resource_id`
- `hourly_rate`
- `currency_code`
- `minimum_hours`
- `booking_interval_minutes`
- `buffer_before_minutes`
- `buffer_after_minutes`
- `default_open_time`
- `default_close_time`
- `max_attendees`
- `deposit_type`
- `deposit_value`
- `cancellation_policy_text`
- `house_rules_text`
- `active`

### `space_bookings`

Booking master record.

Suggested fields:

- `id`
- `booking_number`
- `space_resource_id`
- `status`
- `source`
- `customer_name`
- `customer_phone`
- `customer_email`
- `purpose`
- `attendee_count`
- `subtotal_amount`
- `deposit_amount`
- `paid_amount`
- `balance_amount`
- `note`
- `internal_note`
- `approved_by_user_id`
- `approved_at`
- `cancelled_at`
- `completed_at`
- `created_at`
- `updated_at`

Suggested status values:

- `PENDING`
- `CONFIRMED`
- `REJECTED`
- `CANCELLED`
- `COMPLETED`

Suggested source values:

- `OFFICIAL_WEB`
- `PHONE`
- `WALK_IN`
- `PICKONE`
- `SPACE_PLATFORM`
- `INSTAGRAM`
- `LINE`
- `MANUAL`

### `space_booking_slots`

Stores the actual occupied time ranges.

Suggested fields:

- `id`
- `booking_id`
- `space_resource_id`
- `start_at`
- `end_at`
- `reserved_type`

Suggested `reserved_type`:

- `BOOKING`
- `BUFFER`

This table should be used for conflict checks.

### `space_blockouts`

Manual unavailable ranges.

Suggested fields:

- `id`
- `space_resource_id`
- `title`
- `reason`
- `start_at`
- `end_at`
- `created_by_user_id`
- `created_at`

### `space_booking_payments`

Manual payment tracking for MVP and future gateway integration.

Suggested fields:

- `id`
- `booking_id`
- `payment_type`
- `amount`
- `payment_method`
- `status`
- `paid_at`
- `note`
- `created_by_user_id`
- `created_at`

Suggested payment types:

- `DEPOSIT`
- `BALANCE`
- `REFUND`

## Availability Logic

Public calendar should show only available / unavailable status.

Conflict rules:

- confirmed bookings block the slot
- pending bookings may optionally hold the slot if desired
- blockouts always block the slot
- buffer time also blocks adjacent slots

Suggested rule for MVP:

- `CONFIRMED` blocks the calendar
- `PENDING` does not block publicly unless you want manual hold behavior
- `BLOCKOUT` always blocks

## Pricing Logic

Base formula:

```text
hours * 1000 = subtotal
```

Examples:

- `1 hour = 1000`
- `2 hours = 2000`
- `3.5 hours` should be prevented unless half-hour booking is later supported

For MVP, use whole hours only.

## Public Pages

### `nucosmos.io/space`

Public space landing page.

Sections:

- hero image
- space overview
- equipment / facilities
- pricing
- house rules
- availability calendar
- booking request form
- FAQ

### `nucosmos.io/space/:slug`

Optional future route if multiple spaces are added later.

For MVP, a single fixed page is enough.

## Admin Pages

### `系統管理 / 空間預約`

Recommended sub-pages:

- `Booking Calendar`
- `Booking Requests`
- `Blockout Management`
- `Space Settings`

### Booking Calendar

Functions:

- day / week / month view
- color-coded booking statuses
- click to open booking detail
- create manual booking
- create blockout

### Booking Requests

Functions:

- list pending requests
- filter by status / date / source
- accept / reject / edit

### Blockout Management

Functions:

- create unavailable periods
- edit or remove blockouts

### Space Settings

Functions:

- hourly rate
- open hours
- minimum duration
- buffer time
- capacity
- rules text
- cancellation policy

## API Design

### Public APIs

- `GET /api/v1/public/spaces`
- `GET /api/v1/public/spaces/{slug}`
- `GET /api/v1/public/spaces/{slug}/availability`
- `POST /api/v1/public/spaces/{slug}/booking-requests`

### Admin APIs

- `GET /api/v1/admin/spaces`
- `POST /api/v1/admin/spaces`
- `PATCH /api/v1/admin/spaces/{id}`
- `GET /api/v1/admin/space-bookings`
- `GET /api/v1/admin/space-bookings/{id}`
- `POST /api/v1/admin/space-bookings`
- `PATCH /api/v1/admin/space-bookings/{id}`
- `POST /api/v1/admin/space-bookings/{id}/approve`
- `POST /api/v1/admin/space-bookings/{id}/reject`
- `POST /api/v1/admin/space-bookings/{id}/cancel`
- `GET /api/v1/admin/space-blockouts`
- `POST /api/v1/admin/space-blockouts`
- `PATCH /api/v1/admin/space-blockouts/{id}`
- `DELETE /api/v1/admin/space-blockouts/{id}`

## Notifications

Recommended for phase 2:

- booking request received
- booking approved
- booking rejected
- payment reminder
- booking reminder before event

Delivery options:

- email
- SMS
- LINE notification

## External Platform Handling

External listing platforms should be treated as booking sources, not the source of truth.

Recommended operating rule:

- public availability is primarily controlled by NUCOSMOS internal calendar
- platform bookings must be entered into admin booking calendar immediately
- if sync is added later, sync should still write into the same internal booking tables

## Phase Plan

### Phase 1

- public space page
- public availability calendar
- booking request form
- admin booking calendar
- admin request review
- blockout management

### Phase 2

- deposit / payment management
- email notification
- Google Calendar export / sync
- platform source tracking improvements

### Phase 3

- member-linked booking history
- self-service cancellation
- coupons / promotions
- analytics and occupancy reports

## Recommended Build Order

1. database schema and entities
2. admin APIs
3. public availability API
4. public booking request page
5. admin booking calendar UI
6. blockout UI
7. approval workflow
8. phase 2 payment and notification work
