create table space_resources (
    id uuid primary key,
    store_id uuid not null references stores (id),
    code varchar(50) not null unique,
    name varchar(160) not null,
    slug varchar(120) not null unique,
    description text,
    location_label varchar(160),
    capacity integer not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table space_booking_policies (
    id uuid primary key,
    space_resource_id uuid not null unique references space_resources (id),
    hourly_rate numeric(10, 2) not null,
    currency_code varchar(10) not null,
    minimum_hours integer not null,
    booking_interval_minutes integer not null,
    buffer_before_minutes integer not null,
    buffer_after_minutes integer not null,
    default_open_time time not null,
    default_close_time time not null,
    max_attendees integer not null,
    deposit_type varchar(30) not null,
    deposit_value numeric(10, 2) not null,
    cancellation_policy_text text,
    house_rules_text text,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table space_bookings (
    id uuid primary key,
    space_resource_id uuid not null references space_resources (id),
    approved_by_user_id uuid references users (id),
    booking_number varchar(60) not null unique,
    status varchar(30) not null,
    source varchar(30) not null,
    customer_name varchar(120) not null,
    customer_phone varchar(40) not null,
    customer_email varchar(160),
    purpose varchar(240),
    attendee_count integer not null,
    subtotal_amount numeric(10, 2) not null,
    deposit_amount numeric(10, 2) not null,
    paid_amount numeric(10, 2) not null,
    balance_amount numeric(10, 2) not null,
    note text,
    internal_note text,
    start_at timestamp with time zone not null,
    end_at timestamp with time zone not null,
    approved_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table space_blockouts (
    id uuid primary key,
    space_resource_id uuid not null references space_resources (id),
    created_by_user_id uuid references users (id),
    title varchar(160) not null,
    reason text,
    start_at timestamp with time zone not null,
    end_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_space_resources_store_id on space_resources (store_id);
create index idx_space_bookings_space_id on space_bookings (space_resource_id);
create index idx_space_bookings_window on space_bookings (space_resource_id, start_at, end_at);
create index idx_space_bookings_status on space_bookings (status);
create index idx_space_blockouts_space_id on space_blockouts (space_resource_id);
create index idx_space_blockouts_window on space_blockouts (space_resource_id, start_at, end_at);

insert into space_resources (
    id, store_id, code, name, slug, description, location_label, capacity, active, created_at, updated_at
) values (
    'a5000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'SPACE_2F',
    'NUCOSMOS 2F Event Space',
    '2f-event-space',
    'A flexible upstairs event space for talks, workshops, launches, and private gatherings.',
    'NUCOSMOS 2F',
    30,
    true,
    '2026-04-04T00:00:00+08:00',
    '2026-04-04T00:00:00+08:00'
);

insert into space_booking_policies (
    id, space_resource_id, hourly_rate, currency_code, minimum_hours, booking_interval_minutes,
    buffer_before_minutes, buffer_after_minutes, default_open_time, default_close_time,
    max_attendees, deposit_type, deposit_value, cancellation_policy_text, house_rules_text,
    active, created_at, updated_at
) values (
    'a5000000-0000-0000-0000-000000000002',
    'a5000000-0000-0000-0000-000000000001',
    1000.00,
    'TWD',
    2,
    60,
    0,
    30,
    '10:00:00',
    '22:00:00',
    30,
    'FIXED',
    0.00,
    'For MVP, bookings are reviewed manually. Cancellation and deposit policy will be finalized with the venue terms.',
    'Please use the space responsibly. Commercial shoots, amplified sound, or food service may require prior approval.',
    true,
    '2026-04-04T00:00:00+08:00',
    '2026-04-04T00:00:00+08:00'
);
