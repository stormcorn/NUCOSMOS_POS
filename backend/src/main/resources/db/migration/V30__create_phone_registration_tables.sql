alter table users add column phone_number varchar(30);
alter table users add column phone_verified_at timestamp with time zone;

create unique index uk_users_phone_number on users (phone_number);

create table phone_registration_requests (
    id uuid primary key,
    store_id uuid not null references stores (id),
    phone_number varchar(30) not null,
    pin_hash varchar(255) not null,
    status varchar(40) not null,
    provider varchar(40) not null,
    verification_session_id varchar(255),
    firebase_uid varchar(255),
    verification_completed_at timestamp with time zone,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_phone_registration_store_id on phone_registration_requests (store_id);
create index idx_phone_registration_phone_number on phone_registration_requests (phone_number);
create index idx_phone_registration_status on phone_registration_requests (status);
