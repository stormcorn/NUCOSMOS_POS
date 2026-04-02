create table if not exists receipt_member_trusted_devices (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    member_id uuid not null references receipt_members(id) on delete cascade,
    device_token varchar(128) not null unique,
    device_label varchar(255) not null,
    expires_at timestamptz not null,
    last_authenticated_at timestamptz not null
);

create index if not exists idx_receipt_member_trusted_devices_member_id
    on receipt_member_trusted_devices (member_id);

create index if not exists idx_receipt_member_trusted_devices_expires_at
    on receipt_member_trusted_devices (expires_at);
