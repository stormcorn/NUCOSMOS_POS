alter table receipt_members
    add column if not exists firebase_uid varchar(128);

create unique index if not exists idx_receipt_members_firebase_uid
    on receipt_members (firebase_uid)
    where firebase_uid is not null;

create table if not exists receipt_member_sessions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    member_id uuid not null references receipt_members(id) on delete cascade,
    public_token varchar(96) not null unique,
    expires_at timestamptz not null,
    last_authenticated_at timestamptz not null
);

create index if not exists idx_receipt_member_sessions_member_id
    on receipt_member_sessions (member_id);

create index if not exists idx_receipt_member_sessions_expires_at
    on receipt_member_sessions (expires_at);
